package net.devrieze.chatterbox.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;


public class AuthFilter implements Filter {

  @SuppressWarnings("unused")
  private FilterConfig filterConfig;
  private static final String SECRET = "mad2011";

  @Override
  public void destroy() {
    // Don't do anything yet.
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filterChain) throws IOException, ServletException {
    doFilter((HttpServletRequest)req, (HttpServletResponse) resp, filterChain);
  }
  
  public void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain filterChain) throws IOException, ServletException {
//    System.err.println("dofilter called for "+req.getRequestURI());
    UserService userService = UserServiceFactory.getUserService();
    if (req.getRequestURI().startsWith(loginBaseURL(userService)) || req.getRequestURI().startsWith(logoutBaseURL(userService))) {
      filterChain.doFilter(req, resp);
      return;
    }
    User user = userService.getCurrentUser();
    if (user!=null) {
      if (isAllowed(user)) {
        filterChain.doFilter(req, resp);
        return;
      } else {
        String extramsg=""; 
        if ("POST".equals(req.getMethod())){
          String token = req.getParameter("key");
          if (SECRET.equals(token)) {
            addAllowedUser(user);
            resp.sendRedirect(resp.encodeRedirectURL(req.getRequestURI()));
            return;
          } else {
            extramsg="The token is not right, you will not be authorized to use this app.";
          }
        }
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>\n<html><head><title>Provide access token</title></head><body>");
        out.print("<div style='margin:5em; border: 1px solid black; padding: 2em;'><div style='margin-bottom:2em;'>");
        out.print(extramsg);
        out.print("</div><div><form method='POST' action='"+req.getRequestURI()+"'>");
        out.println("Please provide your access token</div><div><input type='text' name='key' /><button type='submit'>Submit</button></form></div>");
        out.println("<div style='margin-top: 1em;'>You are logged in as "+user.getNickname()+"("+user.getEmail()+") <a href=\""+userService.createLogoutURL(req.getRequestURI())+"\">logout</a></div></div>");
        out.println("</body></html>");
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
    } else {
      //resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
      PrintWriter out = resp.getWriter();
      out.println("<!DOCTYPE html>\n<html><head><title>Please login</title></head><body>");
      out.print("<div style='margin:5em; border: 1px solid black; padding: 2em;'><div style='margin-bottom:2em;'>");
      out.print("Please <a href=\"");
      if (req.getRequestURI().endsWith("logout")) {
        out.print(userService.createLoginURL("/"));
      } else {
        out.print(userService.createLoginURL(req.getRequestURI()));
      }
      out.print("\">login</a></div>");
      out.println("</body></html>");
      
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
  }

  private String loginBaseURL(UserService userService) {
    String s = userService.createLoginURL("");
    int i = s.indexOf('?');
    if (i>=0) {
      return s.substring(0,i);
    }
    return s;
  }

  private String logoutBaseURL(UserService userService) {
    String s = userService.createLogoutURL("");
    int i = s.indexOf('?');
    if (i>=0) {
      return s.substring(0,i);
    }
    return s;
  }

  private void addAllowedUser(User user) {
    PersistenceManager pm = ChatterboxServlet.getPMF().getPersistenceManager();
    try {
      UserList userList = getUserList(pm);
      userList.getPrincipals().add(user.getUserId());
    } finally {
      pm.close();
    }
  }

  private boolean isAllowed(User user) {
    PersistenceManager pm = ChatterboxServlet.getPMF().getPersistenceManager();
    try {
      UserList userList = getUserList(pm);
      return userList.contains(user.getUserId());
    } finally {
      pm.close();
    }
  }

  private UserList getUserList(PersistenceManager pm) {
    try {
      return pm.getObjectById(UserList.class, UserList.DEFAULTKEY);
    } catch (JDOObjectNotFoundException e) {
      return pm.makePersistent(new UserList());
    }
  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {
    this.filterConfig = arg0;
  }

}
