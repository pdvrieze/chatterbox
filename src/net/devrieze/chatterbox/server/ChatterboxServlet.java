package net.devrieze.chatterbox.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ChatterboxServlet extends HttpServlet {

  static final String DEFAULT_BOX = "defaultBox";

  private static final String DEFAULT_OWNER = "pdvrieze";

  private static final long serialVersionUID = 3717262307787043062L;

  private ChannelManager channelManager = new ChannelManager();
  private static enum Method {
    GET,
    POST,
    DELETE,
    PUT;
  }

  private static enum Target {
    LOGOUT("/logout") {
      @Override
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (method == Method.GET) {
          return servlet.handleLogout(req, resp);
        }
        return false;
      }
    },

    CONNECT("/connect") {
      @Override
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        /*if (method == Method.POST) {
          return servlet.handleConnect(req, resp);
        }*/
        return false;
      }
    },

    MESSAGES("/messages"){
      @Override
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

    TOKENS("/tokens") {
      @Override
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (method) {
          case GET:
            return servlet.handleGetAuthTokens(req,resp);
          case DELETE:
            return servlet.handleDelAuthTokens(req,resp);
        }
        return false;
      }

      @Override
      public boolean isTargetted(String myPath) {
        boolean result= TOKENS.prefix.equals(myPath) || (
            myPath!=null &&
            myPath.startsWith(TOKENS.prefix+"/"));
        return result;
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
      @Override
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (method == Method.GET) {
          return servlet.handleBoxes(req, resp);
        }
        return false;
      }
    },
    DEFAULT("/"){

      @Override
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

  private Box getDefaultBox() {
    Box result = ChatboxManager.getBox(DEFAULT_BOX);
    if (result==null) { 
      result = ChatboxManager.createBox(DEFAULT_BOX, DEFAULT_OWNER);
    }
    return result;
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
    resp.setContentType("text/xml; charset=utf-8");
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
    Message m = channelManager.createNewMessageAndNotify(Util.sanitizeHtml(message.toString()), req.getUserPrincipal());
    resp.getWriter().append("<?xml version=\"1.0\"?>\n").append(m.toXML());
    resp.setStatus(HttpServletResponse.SC_OK);
    return true;
  }

  protected boolean handleGetAuthTokens(HttpServletRequest pReq, HttpServletResponse resp) throws IOException {
    if (! ChatboxManager.isSystemAdmin(pReq.getUserPrincipal())) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return true;
    }
    resp.setContentType("text/xml; charset=utf-8");
    PrintWriter out = resp.getWriter();
    out.append("<?xml version=\"1.0\"?>\n");
    out.append("<authTokens>\n");
    for(String s:ChatboxManager.getAuthTokens()){
      out.append("  <authToken>").append(s).append("</authToken>\n");
    }
    out.append("</authTokens>");
    return true;
  }

  protected boolean handleDelAuthTokens(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    return false;
//    resp.setContentType("text/xml; charset=utf-8");
//    PrintWriter out = resp.getWriter();
//    out.append("<?xml version=\"1.0\"?>\n");
//    out.append("<authTokens>\n");
//    Set<String> secrets = PersistenceHandler.getSettings().getSecrets();
//    for(String s:secrets){
//      out.append("  <authToken>").append(s).append("</authToken>\n");
//    }
//    out.append("</authTokens>");
//    return true;
  }

  protected boolean handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.sendRedirect(UserManager.createLogoutURL("/"));
    return true;
  }

  private boolean handleFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//    System.err.println("handlefile called for path "+req.getPathTranslated());
    String path = req.getPathTranslated();
    if (path==null) { path = req.getServletPath(); }
    else if (! path.startsWith("/")) {
      return false;
    }
    path = path.substring(1);
    if (path.length()==0) { path = "Chatterbox.html"; }
    if (path.contains("..")) { return false; }
    
    ServletContext context = req.getSession().getServletContext();
    String path2=context.getRealPath(path);

    FileReader in = new FileReader(path2);
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
    resp.setContentType("text/xml; charset=utf-8");
    PrintWriter out = resp.getWriter();
    out.println("<?xml version=\"1.0\"?>");
    
    try {
      out.print("<messages name=\"");
      Box b;
      try {
        b = getDefaultBox();
        out.print(Util.encodeHtml(b.getName()));
      } finally {
        out.println("\">");
      }
      
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
      out.println("</messages>");
    }
    resp.setStatus(HttpServletResponse.SC_OK);

    return true;
  }

  private boolean handleBoxes(HttpServletRequest pReq, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/xml; charset=utf-8");
    PrintWriter out = resp.getWriter();
    out.println("<?xml version=\"1.0\"?>");
    out.println("<boxes>");
    try {
      for (Box b:ChatboxManager.getBoxes()) {
        out.print("  <box");
        final CharSequence name = b.getName();
        if (DEFAULT_BOX.equals(name)) {
          out.print(" default=\"true\"");
        }
        out.print('>');
        out.print(Util.encodeHtml(name));
        out.println("</box>");
      }
    } finally {
      out.println("</boxes>");
    }
    resp.setStatus(HttpServletResponse.SC_OK);
    return true;
  }

  private boolean handleClear(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    return handleClear(req, resp, getDefaultBox());
  }  
  
  private boolean handleClear(HttpServletRequest pReq, HttpServletResponse resp, Box pBox) throws IOException {
    if (! ChatboxManager.isAdmin(pBox, pReq.getUserPrincipal())) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return true;
    }

    pBox.clear();

    resp.setContentType("text/xml; charset=utf-8");
    PrintWriter out = resp.getWriter();
    out.println("<?xml version=\"1.0\"?>");
    out.println("<messages>");
    out.println("</messages>");
    resp.setStatus(HttpServletResponse.SC_OK);
    return true;
  }
  
/*
  private boolean handleConnect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String response = channelManager.createChannel();
    resp.setContentType("text/xml; charset=utf-8");
    resp.getWriter().append("<?xml version=\"1.0\"?>\n").append(response);
    resp.setStatus(HttpServletResponse.SC_OK);
    return true;
  }
*/
  
  private boolean handleUserInfo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/xml");
    PrintWriter out = resp.getWriter();
    Principal p = req.getUserPrincipal();
    out.println("<?xml version=\"1.0\"?>");
    out.append("<user id=\"").append(Util.encodeHtml(p.getName())).append("\">\n  <email>")
       .append(Util.encodeHtml(p.getName())).append("@localhost</email>\n")
       .append("</user>");

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
