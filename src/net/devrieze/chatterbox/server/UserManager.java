package net.devrieze.chatterbox.server;

import java.security.Principal;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import static net.devrieze.util.DBHelper.*;

import net.devrieze.util.DBHelper;

public class UserManager {
  
  private static final String RESOURCE_REF = "java:/comp/env/jdbc/webauth";

  private static final String APPNAME = "chatterbox";

  private static final String COL_APP = "app";

  private static final String COL_USER = "user";

  private static final String TABLE_PERMS = "APP_PERMS";

  private static final String SQL_ADD_APP_PERM = "INSERT IGNORE INTO "+TABLE_PERMS+" SET "+COL_USER+" = ?, "+COL_APP+" = ?";
  private static final int SQL_I_ADD_APP_PERM_COL_USER = 1;
  private static final int SQL_I_ADD_APP_PERM_COL_APPNAME = 2;
  
  private static final String SQL_CHECK_APP_PERM = "SELECT "+COL_USER+" FROM "+TABLE_PERMS+" WHERE "+COL_USER+"=? AND "+COL_APP+" = ?";
  private static final int SQL_I_CHECK_APP_PERM_COL_USER = 1;
  private static final int SQL_I_CHECK_APP_PERM_COL_APPNAME = 2;

  static DataSource aDataSource;

  public static void addAllowedUser(Principal pPrincipal) {
    dbHelper(RESOURCE_REF).makeInsert(SQL_ADD_APP_PERM, "Failure to register access permission in database")
          .addParam(SQL_I_ADD_APP_PERM_COL_USER, pPrincipal.getName())
          .addParam(SQL_I_ADD_APP_PERM_COL_APPNAME, APPNAME)
          .execCommit();
  }

  public static boolean isAllowedUser(Principal pPrincipal) {
    final boolean result = dbHelper(RESOURCE_REF).makeQuery(SQL_CHECK_APP_PERM, "Failure to verify access permission in database")
          .addParam(SQL_I_CHECK_APP_PERM_COL_USER, pPrincipal.getName())
          .addParam(SQL_I_CHECK_APP_PERM_COL_APPNAME, APPNAME)
          .execQueryNotEmpty();
    return result;
  }

  public static String createLogoutURL(String pURI) {
    return pURI;
  }

  public static String getCurrentUserEmail(Principal p) {
    return p.getName()+"@localhost";
  }

}
