package net.devrieze.chatterbox.server;

import java.net.URI;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import static net.devrieze.util.DBHelper.dbHelper;

public class UserManager {
  
  private static final String RESOURCE_REF = "java:/comp/env/jdbc/webauth";

  private static final String APPNAME = "chatterbox";

  private static final String COL_APP = "app";

  private static final String COL_USER = "user";

  private static final String TABLE_PERMS = "APP_PERMS";

  private static final String SQL_ADD_APP_PERM = "INSERT IGNORE INTO "+TABLE_PERMS+" SET "+COL_USER+" = ?, "+COL_APP+" = ?";
  private static final int SQL_I_ADD_APP_PERM_COL_USER = 0;
  private static final int SQL_I_ADD_APP_PERM_COL_APPNAME = 1;
  
  private static final String SQL_CHECK_APP_PERM = "SELECT "+COL_USER+" FROM "+TABLE_PERMS+" WHERE "+COL_USER+"=? AND "+COL_APP+" = ?";
  private static final int SQL_I_CHECK_APP_PERM_COL_USER = 0;
  private static final int SQL_I_CHECK_APP_PERM_COL_APPNAME = 1;

  static DataSource aDataSource;

  public static void addAllowedUser(Principal pPrincipal) {
    dbHelper(RESOURCE_REF).makeInsert(SQL_ADD_APP_PERM, "Failure to register access permission in database")
          .addParam(SQL_I_ADD_APP_PERM_COL_USER, pPrincipal.getName())
          .addParam(SQL_I_ADD_APP_PERM_COL_APPNAME, APPNAME)
          .execCommit();
  }

  public static boolean isAllowedUser(Principal pPrincipal) {
    ResultSet result = dbHelper(RESOURCE_REF).makeQuery(SQL_ADD_APP_PERM, "Failure to register access permission in database")
          .addParam(SQL_I_ADD_APP_PERM_COL_USER, pPrincipal.getName())
          .addParam(SQL_I_ADD_APP_PERM_COL_APPNAME, APPNAME)
          .execQuery();
    
    
    DataSource dataSource;
    try {
      dataSource = getDataSource();
    } catch (NamingException e) {
      Logger.getGlobal().log(Level.SEVERE, "Failure to get access to datasource", e);
      return false;
    }
    try {
      Connection connection = dataSource.getConnection();
      PreparedStatement stmt = connection.prepareStatement(SQL_CHECK_APP_PERM);
      stmt.setString(SQL_I_CHECK_APP_PERM_COL_USER, pPrincipal.getName());
      stmt.setString(SQL_I_CHECK_APP_PERM_COL_APPNAME, APPNAME);
      ResultSet result = stmt.executeQuery();
      return result.next(); // Just want to know that there is one item
    } catch (SQLException e) {
      Logger.getGlobal().log(Level.SEVERE, "Failure to query access permission in database", e);
      return false;
    }
  }

  public static String createLogoutURL(String pURI) {
    return pURI;
  }

}
