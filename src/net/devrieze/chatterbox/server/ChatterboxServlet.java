package net.devrieze.chatterbox.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;


public class ChatterboxServlet extends HttpServlet {

  private static final long serialVersionUID = 3717262307787043062L;
  
  private static PersistenceManagerFactory _pmf = null;

  public static PersistenceManagerFactory getPMF() {
    if (_pmf != null)
      return _pmf;
    synchronized(ChatterboxServlet.class) {
      if (_pmf != null)
        return _pmf;
      _pmf = JDOHelper.getPersistenceManagerFactory("transactions-optional");
    }
    return _pmf;
  }

  
  private ChannelManager channelManager = new ChannelManager();
  private static enum Method {
    GET,
    POST, 
    DELETE, 
    PUT;
  }
  
  private static enum Target {
    LOGOUT("/logout") {
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (method == Method.GET) {
          return servlet.handleLogout(req, resp);
        }
        return false;
      }
    },
    
    CONNECT("/connect") {
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (method == Method.POST) {
          return servlet.handleConnect(req, resp);
        }
        return false;
      }
    },
    
    MESSAGES("/messages"){
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (method) {
          case GET:
            return servlet.handleMessages(req, resp);
          case POST:
            return servlet.handleMessage(req, resp);
          case DELETE:
            return servlet.handleClear(req, resp);
        }
        return false;
      }
    },
    
    ME("/me"){

      @Override
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (method) {
          case GET:
            return servlet.handleUserInfo(req, resp);
        }
        return false;
      }
      
    },
    
    BOXES("/boxes"){
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (method == Method.GET) {
          return servlet.handleBoxes(req, resp);
        }
        return false;
      }
    },
    DEFAULT("/"){
      
      public boolean isTargetted(String target) {
        return target.endsWith(".html")|| "/".equals(target);
      }

      @Override
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (method == Method.GET) {
          return servlet.handleFile(req, resp);
        }
        return false;
      }
      
    }
    ;
    
    private String prefix;
    
    Target(String prefix) {
      this.prefix = prefix;
    }
    
    public abstract boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException;

    public boolean isTargetted(String myPath) {
      return prefix.equals(myPath);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Target t = getTarget(req);
    if (t==null || (! t.handle(this, Method.GET, req, resp))) {
      super.doGet(req, resp);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Target t = getTarget(req);
    if (t==null || (! t.handle(this, Method.POST, req, resp))) {
      super.doPost(req, resp);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Target t = getTarget(req);
    if (t==null || (! t.handle(this, Method.DELETE, req, resp))) {
      super.doDelete(req, resp);
    }
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Target t = getTarget(req);
    if (t==null || (! t.handle(this, Method.PUT, req, resp))) {
      super.doPut(req, resp);
    }
  }

  private Box getDefaultBox(PersistenceManager pm) {
    try {
      return pm.getObjectById(Box.class, "defaultBox");
    } catch (JDOObjectNotFoundException e) {
      return pm.makePersistent(new Box("defaultBox"));
    }
  }
  
  

  private boolean handleMessage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    int contentLength = req.getContentLength();
    if (contentLength > 10240) {
      resp.setContentType("text/html");
      resp.getWriter().println("<html><head><title>Error, too long</title></head><body><p>Message of length "+
          contentLength+" is longer than 10 kilobytes.</p></body></html>");
      resp.setStatus(414);
      return true;
    }
    resp.setContentType("text/xml");
    BufferedReader in = req.getReader();
    
    // Assume most text will be ascii and as such contentlength == string length
    char[] buffer = new char[contentLength];
    StringBuilder message = new StringBuilder(contentLength);
    {
      int read =  in.read(buffer);
      while (read >=0) {
        message.append(buffer,0,read);
        read = in.read(buffer);
      }
    }
    Message m = channelManager.createNewMessageAndNotify(Util.sanitizeHtml(message.toString()));
    resp.getWriter().append("<?xml version=\"1.0\"?>\n").append(m.toXML());
    resp.setStatus(HttpServletResponse.SC_OK);
    return true;
  }

  protected boolean handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.sendRedirect(UserServiceFactory.getUserService().createLogoutURL("/"));
    return true;
  }

  private boolean handleFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//    System.err.println("handlefile called for path "+req.getPathTranslated());
    String path = req.getPathTranslated();
    if (path==null) { path = req.getRequestURI(); } 
    else if (! path.startsWith("/")) {
      return false;
    }
    path = path.substring(1);
    if (path.length()==0) { path = "Chatterbox.html"; }
    if (path.contains("..")) { return false; }
    
    FileReader in = new FileReader(path);
    char[] buffer = new char[10000];
    StringBuilder result = new StringBuilder(); 
    int c = in.read(buffer);
    while (c>=0) {
      result.append(buffer,0,c);
      c = in.read(buffer);
    }
    resp.getWriter().append(result);
    resp.setStatus(HttpServletResponse.SC_OK);
    return true;
  }

  private boolean handleMessages(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    PrintWriter out = resp.getWriter();
    resp.setContentType("text/xml");
    out.println("<?xml version=\"1.0\"?>");
    out.println("<messages>");
    PersistenceManager pm = getPMF().getPersistenceManager();
    try {
      Box b = getDefaultBox(pm);
      long start = b.getFirstMessageIndex();
      long end = b.getLastMessageIndex();
      {
        String startAttr = req.getParameter("start");
        if("last".equalsIgnoreCase(startAttr)) {
          start=end;
        } else {
          try {
            if (startAttr!=null) { start = Math.max(start, Long.parseLong(startAttr)); }
          } catch (NumberFormatException e) { /* Just ignore */ }
        }
      }
      {
        String endAttr = req.getParameter("end");
        if ("first".equalsIgnoreCase(endAttr)) {
          end=b.getFirstMessageIndex();
        } else {
          try {
            if (endAttr!=null) { end = Math.min(end, Long.parseLong(endAttr)); }
          } catch (NumberFormatException e) { /* Just ignore */ }
        }
      }
      
      for (Message m:b.getMessages(start, end)) {
        out.println(m.toXML());
      }
      
    } finally {
      pm.close();
      out.println("</messages>");
    }
    resp.setStatus(HttpServletResponse.SC_OK);
    
    return true;
  }

  private boolean handleBoxes(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    PrintWriter out = resp.getWriter();
    resp.setContentType("text/xml");
    out.println("<?xml version=\"1.0\"?>");
    out.println("<boxes>");
    out.println("  <box default=\"true\">main</box>");
    out.println("</boxes>");
    resp.setStatus(HttpServletResponse.SC_OK);
    return true;
  }

  private boolean handleClear(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    if (! UserServiceFactory.getUserService().isUserAdmin()) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return true;
    }
    PersistenceManager pm = getPMF().getPersistenceManager();
    try {
      Box b = getDefaultBox(pm);
      b.clear();
    } finally {
      pm.close();
    }
    PrintWriter out = resp.getWriter();
    resp.setContentType("text/xml");
    out.println("<?xml version=\"1.0\"?>");
    out.println("<messages>");
    out.println("</messages>");
    resp.setStatus(HttpServletResponse.SC_OK);
    return true;
  }

  private boolean handleConnect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String response = channelManager.createChannel();
    resp.getWriter().append("<?xml version=\"1.0\"?>\n").append(response);
    resp.setContentType("text/xml");
    resp.setStatus(HttpServletResponse.SC_OK);
    return true;
  }

  private boolean handleUserInfo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    PrintWriter out = resp.getWriter();
    resp.setContentType("text/xml");
    out.println("<?xml version=\"1.0\"?>");
    UserService userService = UserServiceFactory.getUserService();
    String logout = userService.createLogoutURL("/");
    User user = userService.getCurrentUser();
    out.append("<user id=\"").append(Util.encodeHtml(user.getUserId())).append("\">\n  <email>");
    out.append(Util.encodeHtml(user.getEmail())).append("</email>\n  <logout>");
    out.append(logout).append("</logout>\n");
    if (user.getNickname()!=null) {
      out.append("  <nickname>").append(Util.encodeHtml(user.getNickname())).append("</nickname>\n");
    }
    out.append("</user>");
    
    return true;
  }

  private Target getTarget(HttpServletRequest req) {
    String myPath = req.getPathInfo();
    if (myPath==null) {
      myPath=req.getRequestURI();
    }
    for (Target t:Target.values()) {
      if(t.isTargetted(myPath)) {
        return t;
      }
    }
    return null;
  }
  
  
  
}
