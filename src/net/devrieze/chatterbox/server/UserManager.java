package net.devrieze.chatterbox.server;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class UserManager {
  
  private static final String APPNAME = "chatterbox";

  private static final String COL_APP = "app";

  private static final String COL_USER = "user";

  private static final String TABLE_PERMS = "APP_PERMS";

  private static final String SQL_ADD_APP_PERM = "INSERT INTO "+TABLE_PERMS+" SET "+COL_USER+" = ?, "+COL_APP+" = ?";
  private static final int SQL_I_ADD_APP_PERM_COL_USER = 0;
  private static final int SQL_I_ADD_APP_PERM_COL_APPNAME = 1;

  static DataSource aDataSource;

  private static final String RESOURCE_REF = "java:/comp/env/jdbc/webauth";

  public static void addAllowedUser(Principal pPrincipal) {
    DataSource dataSource;
    try {
      dataSource = getDataSource();
    } catch (NamingException e) {
      Logger.getGlobal().log(Level.SEVERE, "Failure to get access to datasource", e);
      return;
    }
    try {
      Connection connection = dataSource.getConnection();
      PreparedStatement stmt = connection.prepareStatement(SQL_ADD_APP_PERM);
      stmt.setString(SQL_I_ADD_APP_PERM_COL_USER, pPrincipal.getName());
      stmt.setString(SQL_I_ADD_APP_PERM_COL_APPNAME, APPNAME);
      
    } catch (SQLException e) {
      Logger.getGlobal().log(Level.SEVERE, "Failure to register access permission in database", e);
      return;
    }
  }
  
  public static DataSource getDataSource() throws NamingException {
    InitialContext initialContext = new InitialContext();
    return (DataSource) initialContext.lookup(RESOURCE_REF);
  }

}
