package net.devrieze.chatterbox.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.CharBuffer;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;


public class ChatterboxServlet extends HttpServlet {

  private static final long serialVersionUID = 3717262307787043062L;
  
  private static final String SECRET = "mad2011";

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
    CONNECT("/connect") {
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) {
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
    
    CLEAR("/clear"){
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (method == Method.POST || method == Method.GET) {
          return servlet.handleClear(req, resp);
        }
        return false;
      }
    },
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
      System.out.println("<html><head><title>Error, too long</title></head><body><p>Message of length "+
          contentLength+" is longer than 10 kilobytes.</p></body></html>");
      resp.setStatus(HttpStatus.SC_REQUEST_TOO_LONG);
      return true;
    }
    resp.setContentType("text/xml");
    Reader r = req.getReader();
    // Assume most text will be ascii and as such contentlength == string length
    CharBuffer message = CharBuffer.allocate(contentLength);
    r.read(message);
    Message m = channelManager.sendMessageToChannels(message.toString());
    resp.getWriter().append(m.toXML());
    resp.setStatus(HttpStatus.SC_OK);
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
    resp.setStatus(HttpStatus.SC_OK);
    
    return true;
  }

  private boolean handleBoxes(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    PrintWriter out = resp.getWriter();
    resp.setContentType("text/xml");
    out.println("<?xml version=\"1.0\"?>");
    out.println("<boxes>");
    out.println("  <box default=\"true\">main</box>");
    out.println("</boxes>");
    resp.setStatus(HttpStatus.SC_OK);
    return true;
  }

  private boolean handleClear(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
    resp.setStatus(HttpStatus.SC_OK);
    return true;
  }

  private boolean handleConnect(HttpServletRequest req, HttpServletResponse resp) {
    // TODO Auto-generated method stub
    return false;
  }

  private Target getTarget(HttpServletRequest req) {
    String myPath = req.getPathInfo();
    if (myPath==null) {
      return null;
    }
    for (Target t:Target.values()) {
      if(t.isTargetted(myPath)) {
        return t;
      }
    }
    
    if (myPath.endsWith("/")) {
      myPath = myPath.substring(0, myPath.length()-1);
    }
    if ("/connect".equals(myPath)) {
      return Target.CONNECT;
    }
    if ("/clear".equals(myPath)) {
      return Target.CONNECT;
    }
    if ("/messages".equals(myPath)) {
      return Target.MESSAGES;
    }
    if (myPath.startsWith("/boxes/") && myPath.length()>7) {
      return Target.BOXES;
    }
    return null;
  }
  
  
  
}
