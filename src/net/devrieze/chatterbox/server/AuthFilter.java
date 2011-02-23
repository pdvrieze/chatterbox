package net.devrieze.chatterbox.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

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

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;


public class AuthFilter implements Filter {

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

    /*if ("127.0.0.1".equals(req.getRemoteAddr())) {
      filterChain.doFilter(req, resp);
    }*/
    UserService userService = UserServiceFactory.getUserService();
    if (userService.createLoginURL(req.getRequestURI()).startsWith(req.getRequestURI())) {
      filterChain.doFilter(req, resp);
      return;
    }
    Principal principal = req.getUserPrincipal();
    if (principal!=null) {
      if (isAllowed(principal)) {
        if (req.getPathInfo().startsWith("/chat/clear")) {
          if (isAdmin(principal)) {
            filterChain.doFilter(req, resp);
            return;
          }
        } else {
          filterChain.doFilter(req, resp);
          return;
        }
      } else {
        String extramsg=""; 
        if ("POST".equals(req.getMethod())){
          String token = req.getParameter("key");
          if (SECRET.equals(token)) {
            addAllowedPrincipal(principal);
            resp.encodeRedirectURL(req.getRequestURI());
            return;
          } else {
            extramsg="The token is not right, you will not be authorized to use this app.";
          }
        }
        PrintWriter out = resp.getWriter();
        out.println("<html><head><title>Provide access token</title></head><body>");
        out.print("<div style='margin:5em; border: 1px solid black; padding: 2em;'><div style='margin-bottom:2em;'>");
        out.print(extramsg);
        out.print("</div><div><form method='POST' action='"+req.getRequestURI()+"'>");
        out.println("Please provide your access token</div><div><input type='text' name='key' /><button type='submit'>Submit</button></form></div>");
        out.println("</div>");
        out.println("</body></html>");
        resp.setStatus(HttpServletResponse.SC_OK);
      }
    } else {
      PrintWriter out = resp.getWriter();
      out.println("<html><head><title>Please login</title></head><body>");
      out.print("<div style='margin:5em; border: 1px solid black; padding: 2em;'><div style='margin-bottom:2em;'>");
      out.print("Please <a href=\""+userService.createLoginURL(req.getRequestURI())+"\">login</a></div>");
      out.println("</body></html>");
      
      resp.setStatus(HttpServletResponse.SC_OK);
    }
    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
  }

  private void addAllowedPrincipal(Principal principal) {
    PersistenceManager pm = ChatterboxServlet.getPMF().getPersistenceManager();
    try {
      PrincipalList principalList = getPrincipalList(pm);
      principalList.getPrincipals().add(principal.toString());
    } finally {
      pm.close();
    }
  }

  private boolean isAdmin(Principal principal) {
    return "paul.devrieze@gmail.com".equals(principal.toString());
  }

  private boolean isAllowed(Principal principal) {
    PersistenceManager pm = ChatterboxServlet.getPMF().getPersistenceManager();
    try {
      PrincipalList principalList = getPrincipalList(pm);
      return principalList.contains(principal.toString());
    } finally {
      pm.close();
    }
  }

  private PrincipalList getPrincipalList(PersistenceManager pm) {
    try {
      return pm.getObjectById(PrincipalList.class, PrincipalList.DEFAULTKEY);
    } catch (JDOObjectNotFoundException e) {
      return pm.makePersistent(new PrincipalList());
    }
  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {
    this.filterConfig = arg0;
  }

}
