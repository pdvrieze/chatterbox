package net.devrieze.chatterbox.server;

import static org.atmosphere.cpr.AtmosphereResource.TRANSPORT.LONG_POLLING;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.devrieze.util.DBHelper;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.Meteor;

import uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipal;


public class ChatterboxServlet extends HttpServlet {

  static final String DEFAULT_BOX = "defaultBox";

  private static final String DEFAULT_OWNER = "pdvrieze";

  private static final long serialVersionUID = 3717262307787043062L;

  private static final Object MIME_TYPE_COMET = "application/comet";

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
        switch (method) {
          case GET:
            return servlet.handleLogout(resp);
          default:
            return false;
        }
      }
    },

    CONNECT("/comet") {
      @Override
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (method) {
          case GET:
            return servlet.handleConnect(req, resp);
          default:
            return false;
        }
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
          default:
            return false;
        }
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
          default:
            return false;
        }
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
          default:
            return false;
        }
      }

    },

    BOXES("/boxes"){
      @Override
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (method) {
          case GET:
            return servlet.handleBoxes(req, resp);
          default:
            return false;
        }
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
    if (t==null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    if (t.handle(this, Method.GET, req, resp)) {
      DBHelper.closeConnections(req);
    } else {
      super.doGet(req, resp);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Target t = getTarget(req);
    if (t==null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    if ((t.handle(this, Method.POST, req, resp))) {
      DBHelper.closeConnections(req);
    } else {
      super.doPost(req, resp);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Target t = getTarget(req);
    if (t==null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    if ((t.handle(this, Method.DELETE, req, resp))) {
      DBHelper.closeConnections(req);
    } else {
      super.doDelete(req, resp);
    }
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Target t = getTarget(req);
    if (t==null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    if ((t.handle(this, Method.PUT, req, resp))) {
      DBHelper.closeConnections(req);
    } else {
      super.doPut(req, resp);
    }
  }

  private Box getDefaultBox(ServletRequest pKey) {
    Box result = ChatboxManager.getBox(DEFAULT_BOX, pKey);
    if (result==null) { 
      result = ChatboxManager.createBox(DEFAULT_BOX, DEFAULT_OWNER, pKey);
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
    Message m = channelManager.createNewMessageAndNotify(Util.sanitizeHtml(message.toString()), req.getUserPrincipal(), req);
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
    for(String s:ChatboxManager.getAuthTokens(pReq)){
      out.append("  <authToken>").append(s).append("</authToken>\n");
    }
    out.append("</authTokens>");
    return true;
  }

  protected boolean handleDelAuthTokens(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    if (! ChatboxManager.isSystemAdmin(req.getUserPrincipal())) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return true;
    }
    String token = req.getPathInfo();
    boolean notFound;
    if (! token.startsWith(Target.TOKENS.prefix)) {
      notFound=true;
    } else {
      token = token.substring(Target.TOKENS.prefix.length());
      if (! (token.length()>=2 && token.charAt(0)=='/')) {
        notFound= true;
      } else {
        token = token.substring(1);
        notFound = ! ChatboxManager.isValidToken(token, req);
      }
    }
    
    if (notFound) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return true;
    } else {
      ChatboxManager.removeAuthToken(token, req);
      handleGetAuthTokens(req, resp);
      return true;
    }
  }

  protected boolean handleLogout(HttpServletResponse resp) throws IOException {
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
    try {
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
    } finally {
      in.close();
    }
  }

  private boolean handleMessages(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/xml; charset=utf-8");
    PrintWriter out = resp.getWriter();
    out.println("<?xml version=\"1.0\"?>");
    
    try {
      out.print("<messages name=\"");
      Box b;
      try {
        b = getDefaultBox(req);
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

  private boolean handleBoxes(HttpServletRequest pReq, HttpServletResponse pResp) throws IOException {
    pResp.setContentType("text/xml; charset=utf-8");
    PrintWriter out = pResp.getWriter();
    out.println("<?xml version=\"1.0\"?>");
    out.println("<boxes>");
    try {
      for (Box b:ChatboxManager.getBoxes(pReq)) {
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
    pResp.setStatus(HttpServletResponse.SC_OK);
    return true;
  }

  private boolean handleClear(HttpServletRequest pReq, HttpServletResponse pResp) throws IOException {
    return handleClear(pReq, pResp, getDefaultBox(pReq));
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
  

  private boolean handleConnect(HttpServletRequest req, HttpServletResponse resp) {
    Meteor m = Meteor.build(req).addListener(channelManager);
    // TODO do we really need to do this?
    if (MIME_TYPE_COMET.equals(req.getContentType())){
      resp.setContentType("application/comet;charset=utf8");
    }

    Broadcaster b = channelManager.getBroadcaster();
    m.setBroadcaster(b);
    m.resumeOnBroadcast(m.transport() == LONG_POLLING);
    m.suspend(-1,false);
    return true;
  }

  private boolean handleUserInfo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/xml");
    PrintWriter out = resp.getWriter();
    Principal p = req.getUserPrincipal();
    out.println("<?xml version=\"1.0\"?>");
    out.append("<user id=\"").append(Util.encodeHtml(p.getName())).append("\">\n  <email>");
    if (p instanceof DarwinUserPrincipal) {
      out.append(Util.encodeHtml(((DarwinUserPrincipal)p).getEmail())).append("</email>\n");
    } else {
      out.append(Util.encodeHtml(p.getName())).append("@localhost</email>\n");
    }
    out.append("</user>");

    return true;
  }

  private Target getTarget(HttpServletRequest req) {
    String myPath = req.getPathInfo();
    if (myPath==null || myPath.length()==0) {
      myPath=req.getServletPath();
    }
    for (Target t:Target.values()) {
      if(t.isTargetted(myPath)) {
        return t;
      }
    }
    if (myPath!=req.getServletPath()) {
      myPath = req.getServletPath();
      for (Target t:Target.values()) {
        if(t.isTargetted(myPath)) {
          return t;
        }
      }
    }
    return null;
  }



}
