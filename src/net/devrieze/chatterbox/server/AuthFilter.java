package net.devrieze.chatterbox.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class AuthFilter implements Filter {

  private static final String DARWIN_AUTH_COOKIE = "DWNID";
  @SuppressWarnings("unused")
  private FilterConfig filterConfig;
  private Logger logger;
  @Override
  public void destroy() {
    // Don't do anything yet.
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filterChain) throws IOException, ServletException {
    doFilter((HttpServletRequest)req, (HttpServletResponse) resp, filterChain);
  }

  private Logger getLogger() {
    if (logger==null) {
      logger = Logger.getLogger(getClass().getName());
    }
    return logger;
  }

  public void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain filterChain) throws IOException, ServletException {
//    System.err.println("dofilter called for "+req.getRequestURI());
    getLogger().log(Level.FINE, "Calling filter for: "+req.getRequestURI());
    Principal principal = getPrincipal(req);
    if (req.getRequestURI().startsWith("/_ah/login")) {
      getLogger().log(Level.FINER, "Calling not filtering authentication: "+req.getRequestURI());
      filterChain.doFilter(req, resp);
      return;
    }

    // Allow access to messages without authentication
    if ("/messages".equals(req.getServletPath()) && req.getMethod().equals("GET")) {
      filterChain.doFilter(req, resp);
      return;
    }

    if (principal!=null) {
      if (isAllowed(principal, req)) {
        filterChain.doFilter(req, resp);
        return;
      } else {
        String extramsg="";
        if ("POST".equals(req.getMethod())){
          String token = req.getParameter("key");
          if (token!=null && token.length()>0) {
            if (ChatboxManager.isValidToken(token, req)) {
              addAllowedUser(principal, req);
              resp.sendRedirect(resp.encodeRedirectURL(req.getRequestURI()));
              return;
            } else {
              extramsg="The token is not right, you will not be authorized to use this app.";
            }
          } else {
            extramsg="No token received, you will not be authorized to use this app.";
          }
        }
        resp.setContentType("text/html; charset=utf8");
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>\n<html><head><title>Provide access token</title></head><body>");
        out.print("<div style='margin:5em; border: 1px solid black; padding: 2em;'><div style='margin-bottom:2em;'>");
        out.print(extramsg);
        out.print("</div><div><form method='POST' action='"+req.getRequestURI()+"'>");
        out.println("Please provide your access token</div><div><input type='text' name='key' /><button type='submit'>Submit</button></form></div>");
        out.println("<div style='margin-top: 1em;'>You are logged in as "+principal.getName()+"</div></div>");
        out.println("</body></html>");
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
    } else {
      //resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      PrintWriter out = resp.getWriter();
      out.println("<!DOCTYPE html>\n<html><head><title>Please login</title></head><body>");
      out.print("<div style='margin:5em; border: 1px solid black; padding: 2em;'><div style='margin-bottom:2em;'>");
      out.print("Please <a href=\"");
      if (req.getRequestURI().endsWith("logout")) {
        out.print(getLoginURL("/"));
      } else {
        out.print(getLoginURL(req.getRequestURI()));
      }
      out.print("\">login</a></div>");
      out.println("</body></html>");

      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
  }

  private Principal getPrincipal(HttpServletRequest req) {
    Principal result = req.getUserPrincipal();
    if (result!=null) { return result; }
    
    Cookie cookie = getCookie(req, DARWIN_AUTH_COOKIE);
    
    return req.getUserPrincipal();
  }

  private Cookie getCookie(HttpServletRequest pReq, String pCookieName) {
    for (Cookie cookie: pReq.getCookies()) {
      if (cookie.getName()==pCookieName) {
        return cookie;
      }
    }
    return null;
  }

  /**
   * Get the login url that will forward the user to the requested page.
   * @param pRequestURI The url to forward to.
   */
  private String getLoginURL(String pRequestURI) {
    return "";
  }
  
  private static void addAllowedUser(Principal principal, ServletRequest pKey) {
    UserManager.addAllowedUser(principal, pKey);
  }

  private static boolean isAllowed(Principal principal, ServletRequest pKey) {
    return UserManager.isAllowedUser(principal, pKey);
  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {
    this.filterConfig = arg0;
//    Object dbKey = new Object();
//    ChatboxManager.ensureTables(dbKey);
//    try {
//      DBHelper.dbHelper(UserManager.RESOURCE_REF, dbKey).close();
//    } catch (SQLException e) {
//      throw new ServletException(e);
//    }
  }

}
