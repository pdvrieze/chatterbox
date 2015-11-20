package net.devrieze.chatterbox.server;

import org.jetbrains.annotations.NotNull;

import javax.naming.NamingException;
import javax.sql.DataSource;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static net.devrieze.chatterbox.server.Util.getConnection;


public class UserManager {

  static final String RESOURCE_REF = "java:.comp.env.jdbc.webauth";

  private static final String APPNAME = "chatterbox";

  private static final String COL_APP = "`app`";

  private static final String COL_USER = "`user`";

  private static final String TABLE_PERMS = "`app_perms`";

  private static volatile boolean _checkedDarwinPrincipal;
  private static Class<? extends Principal> _DarwinUserPrincipal;
  private static MethodHandle _getEmail;

  @NotNull
  private static final String SQL_ADD_APP_PERM = "INSERT IGNORE INTO "+TABLE_PERMS+" SET "+COL_USER+" = ?, "+COL_APP+" = ?";
  private static final int SQL_I_ADD_APP_PERM_COL_USER = 1;

  private static final int SQL_I_ADD_APP_PERM_COL_APPNAME = 2;
  @NotNull
  private static final String SQL_CHECK_APP_PERM = "SELECT "+COL_USER+" FROM "+TABLE_PERMS+" WHERE "+COL_USER+"=? AND "+COL_APP+" = ?";
  private static final int SQL_I_CHECK_APP_PERM_COL_USER = 1;

  private static final int SQL_I_CHECK_APP_PERM_COL_APPNAME = 2;
  static DataSource aDataSource;

  public static void addAllowedUser(Principal pPrincipal) throws SQLException, NamingException {
    try (Connection connection = getConnection(RESOURCE_REF)){
      try (PreparedStatement insert = connection.prepareStatement(SQL_ADD_APP_PERM)) {
        insert.setString(SQL_I_ADD_APP_PERM_COL_USER, pPrincipal.getName());
        insert.setString(SQL_I_ADD_APP_PERM_COL_APPNAME, APPNAME);
        insert.executeUpdate();
      }
      connection.commit();
    }
  }

  public static boolean isAllowedUser(Principal pPrincipal) throws SQLException, NamingException {
    try (Connection connection = getConnection(RESOURCE_REF)){
      try (PreparedStatement query = connection.prepareStatement(SQL_CHECK_APP_PERM)) {
        query.setString(SQL_I_CHECK_APP_PERM_COL_USER, pPrincipal.getName());
        query.setString(SQL_I_CHECK_APP_PERM_COL_APPNAME, APPNAME);
        try (ResultSet results = query.executeQuery()) {
          return results.next();
        }
      }
    }
  }

  public static String createLogoutURL(String pURI) {
    return ".accounts.logout";
  }

  public static String getCurrentUserEmail(Principal p) {
    if (! _checkedDarwinPrincipal) {
      synchronized (UserManager.class) {
        _checkedDarwinPrincipal = true;
        try {
          //noinspection unchecked
          _DarwinUserPrincipal = (Class<? extends Principal>) UserManager.class.getClassLoader().loadClass("uk.ac.bournemouth.darwin.catalina.realm.DarwinUserPrincipal");
          Lookup lookup = MethodHandles.lookup();
          _getEmail = lookup.findVirtual(_DarwinUserPrincipal, "getEmail", MethodType.methodType(CharSequence.class));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
          _DarwinUserPrincipal = null;
          _getEmail = null;
          // Just make sure these are null and we don't attempt to use them.
        }
      }

    }
    if (_getEmail!=null) {
      if (_DarwinUserPrincipal.isInstance(p)) {
        try {
          return _getEmail.invokeExact(_DarwinUserPrincipal.cast(p)).toString();
        } catch (Throwable throwable) {
          throw new RuntimeException(throwable);
        }
      }
    }

    return p.getName()+"@bournemouth.ac.uk";
  }

  public static void destroy() {
    // nothing
  }

}
