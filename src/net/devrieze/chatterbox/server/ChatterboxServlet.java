package net.devrieze.chatterbox.server;

import net.devrieze.chatterbox.server.ChatboxManager.BoxAdapter;
import net.devrieze.chatterbox.server.ChatboxManager.MessageAdapter;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.Meteor;
import org.jetbrains.annotations.NotNull;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import static org.atmosphere.cpr.AtmosphereResource.TRANSPORT.LONG_POLLING;


public class ChatterboxServlet extends HttpServlet {

  static final String VERSION="1.0.1";

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
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws
              IOException, SQLException, NamingException {
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
      public boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws
              IOException, SQLException, NamingException {
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

    public abstract boolean handle(ChatterboxServlet servlet, Method method, HttpServletRequest req, HttpServletResponse resp) throws
            IOException, SQLException, NamingException;

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
    getLogger().info("Handling GET request for "+t.prefix);
    try {
      if (t.handle(this, Method.GET, req, resp)) {
        getLogger().fine("Handled request succesfully");
      } else {
        getLogger().info("Could not handle request");
        super.doGet(req, resp);
      }
    } catch (NamingException | SQLException e) {
      throw new ServletException(e);
    }
  }

  private Logger getLogger() {
    return Logger.getLogger(ChatterboxServlet.class.getName());
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Target t = getTarget(req);
    if (t==null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    getLogger().info("Handling POST request for "+t.prefix);
    try {
      if (! (t.handle(this, Method.POST, req, resp))) {
        super.doPost(req, resp);
      }
    } catch (NamingException | SQLException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Target t = getTarget(req);
    if (t==null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    getLogger().info("Handling DELETE request for "+t.prefix);
    try {
      if (! (t.handle(this, Method.DELETE, req, resp))) {
        super.doDelete(req, resp);
      }
    } catch (NamingException | SQLException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Target t = getTarget(req);
    if (t==null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    getLogger().info("Handling PUT request for "+t.prefix);
    try {
      if (!(t.handle(this, Method.PUT, req, resp))) {
        super.doPut(req, resp);
      }
    } catch (NamingException | SQLException e) {
      throw new ServletException(e);
    }
  }

  private Box getDefaultBox(@NotNull Connection db) throws SQLException {
    Box result = ChatboxManager.getBox(db, DEFAULT_BOX);
    if (result==null) {
      result = ChatboxManager.createBox(db, DEFAULT_BOX, DEFAULT_OWNER);
    }
    return result;
  }

  private boolean handleMessage(HttpServletRequest req, HttpServletResponse resp) throws IOException, SQLException,
          NamingException {
    int contentLength = req.getContentLength();
    if (contentLength > 10240) {
      resp.setContentType("text/html");
      resp.getWriter().println("<html><head><title>Error, too long</title></head><body><p>Message of length "+
          contentLength+" is longer than 10 kilobytes.</p></body></html>");
      resp.setStatus(414);
      return true;
    }
    resp.setContentType("text/xml; charset=utf-8");

    StringBuilder message = new StringBuilder(contentLength);
    try(BufferedReader in = req.getReader()) {

      // Assume most text will be ascii and as such contentlength == string length
      char[] buffer = new char[contentLength];
      {
        int read =  in.read(buffer);
        while (read >=0) {
          message.append(buffer,0,read);
          read = in.read(buffer);
        }
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
    try(PrintWriter out = resp.getWriter()) {
      out.append("<?xml version=\"1.0\"?>\n");
      out.append("<authTokens>\n");
      try (Connection connection = Util.getConnection(ChatboxManager.CHATBOX_DB_RESOURCE)) {
           Iterable<String> authTokens = ChatboxManager.getAuthTokens(connection);
        for (String s : authTokens) {
          out.append("  <authToken>").append(s).append("</authToken>\n");
        }
      } catch (Exception e) {
        if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        } else {
          throw new RuntimeException(e);
        }
      }
      out.append("</authTokens>");
    }
    return true;
  }

  protected boolean handleDelAuthTokens(HttpServletRequest req, HttpServletResponse resp) throws IOException,
          SQLException, NamingException {
    if (! ChatboxManager.isSystemAdmin(req.getUserPrincipal())) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return true;
    }
    String token = req.getPathInfo();
    boolean notFound;
    @SuppressWarnings("resource")
    Connection db=null;
    try {
      if (! token.startsWith(Target.TOKENS.prefix)) {
        notFound=true;
      } else {
        token = token.substring(Target.TOKENS.prefix.length());
        if (! (token.length()>=2 && token.charAt(0)=='/')) {
          notFound= true;
        } else {
          token = token.substring(1);
          db = Util.getConnection(ChatboxManager.CHATBOX_DB_RESOURCE);
          notFound = ! ChatboxManager.isValidToken(db, token);
        }
      }

      if (notFound) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return true;
      } else {
        if (db==null) {
          db = Util.getConnection(ChatboxManager.CHATBOX_DB_RESOURCE); }
        ChatboxManager.removeAuthToken(db, token);
        handleGetAuthTokens(req, resp);
        return true;
      }
    } finally {
      if (db!=null) { db.close(); }
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

    try (FileReader in = new FileReader(path2)){
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
  }

  private boolean handleMessages(HttpServletRequest req, HttpServletResponse resp) throws IOException, SQLException,
          NamingException {
    resp.setContentType("text/xml; charset=utf-8");
    try (PrintWriter out = resp.getWriter()) {
      out.println("<?xml version=\"1.0\"?>");

      try (Connection connection = Util.getConnection(ChatboxManager.CHATBOX_DB_RESOURCE)){
        out.print("<messages name=\"");
        Box b;
        try {
          b = getDefaultBox(connection);
          out.print(Util.encodeHtml(b.getName()));
        } finally {
          out.println("\">");
        }

        long start = b.getFirstMessageIndex(connection);
        long end = b.getLastMessageIndex(connection);
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
            end=start;
          } else {
            try {
              if (endAttr!=null) { end = Math.min(end, Long.parseLong(endAttr)); }
            } catch (NumberFormatException e) { /* Just ignore */ }
          }
        }

        try (PreparedStatement messagesStatement = b.getMessages(connection, start, end);
             ResultSet messages = messagesStatement.executeQuery()) {
          MessageAdapter adapter = new MessageAdapter(messages.getMetaData());
          while (messages.next()) {
            out.println(adapter.getMessage(messages).toXML());
          }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

      } finally {
        out.println("</messages>");
      }
    }
    resp.setStatus(HttpServletResponse.SC_OK);

    return true;
  }

  private boolean handleBoxes(HttpServletRequest pReq, HttpServletResponse pResp) throws IOException {
    pResp.setContentType("text/xml; charset=utf-8");
    try(PrintWriter out = pResp.getWriter()) {
      out.println("<?xml version=\"1.0\"?>");
      out.println("<boxes>");
      try (Connection connection = Util.getConnection(ChatboxManager.CHATBOX_DB_RESOURCE);
           PreparedStatement boxesQuery = ChatboxManager.getBoxesStatement(connection);
           ResultSet boxes = boxesQuery.executeQuery()) {
        BoxAdapter adapter = new BoxAdapter(boxes.getMetaData());
        while (boxes.next()) {
          Box b = adapter.getBox(boxes);
          out.print("  <box");
          final CharSequence name = b.getName();
          if (DEFAULT_BOX.equals(name)) {
            out.print(" default=\"true\"");
          }
          out.print('>');
          out.print(Util.encodeHtml(name));
          out.println("</box>");
        }
      } catch (NamingException | SQLException e) {
        throw new RuntimeException(e);
      } finally {
        out.println("</boxes>");
      }
      pResp.setStatus(HttpServletResponse.SC_OK);
      return true;
    }
  }

  private boolean handleClear(HttpServletRequest pReq, HttpServletResponse pResp) throws IOException, SQLException,
          NamingException {
    try (Connection connection = Util.getConnection(ChatboxManager.CHATBOX_DB_RESOURCE)){
      return handleClear(connection, pReq, pResp, getDefaultBox(connection));
    }
  }

  private boolean handleClear(@NotNull Connection connection, HttpServletRequest pReq, HttpServletResponse resp, Box pBox) throws IOException, SQLException {
    if (! ChatboxManager.isAdmin(pBox, pReq.getUserPrincipal())) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return true;
    }

    pBox.clear(connection);

    resp.setContentType("text/xml; charset=utf-8");
    try (PrintWriter out = resp.getWriter();) {
      out.println("<?xml version=\"1.0\"?>");
      out.println("<messages>");
      out.println("</messages>");
      resp.setStatus(HttpServletResponse.SC_OK);
      return true;
    }
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
    try(PrintWriter out = resp.getWriter();) {
      Principal p = req.getUserPrincipal();
      out.println("<?xml version=\"1.0\"?>");
      out.append("<user id=\"").append(Util.encodeHtml(p.getName())).append("\">\n  <email>");
      out.append(Util.encodeHtml(UserManager.getCurrentUserEmail(p)));
      out.append("</user>");

      return true;
    }
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

  @Override
  public String getServletName() {
    return ChatterboxServlet.class.getSimpleName();
  }

  @Override
  public void init() throws ServletException {
    super.init();
    getLogger().info(getServletName()+" version:"+VERSION+" started");
  }

  @Override
  public void destroy() {
    ChatboxManager.destroy();
    UserManager.destroy();
    channelManager.destroy();
    super.destroy();
  }



}
