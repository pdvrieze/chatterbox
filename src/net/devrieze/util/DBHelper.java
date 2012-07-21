package net.devrieze.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;


public class DBHelper {
  
  private static class DataSourceWrapper {
    DataSource aDataSource;
    ConcurrentHashMap<Object, Connection> aConnectionMap;
    
    DataSourceWrapper(DataSource pDataSource) {
      aDataSource = pDataSource;
      aConnectionMap = new ConcurrentHashMap<>(5);
    }
  }
  
  
  
  public interface DBStatement {

    DBStatement addParam(int pColumn, String pValue);
    DBStatement addParam(int pColumn, int pValue);
    DBStatement addParam(int pColumn, long pValue);

    boolean exec();

    boolean execCommit();

  }


  public String aErrorMsg;
  public PreparedStatement aSQL;
  private Connection aConnection;
  private Object aKey;
  public boolean aValid;
  
  private static Object aShareLock = new Object();
  private static final boolean SHARE_CONNECTIONS = true;
  private volatile static Map<String, DataSourceWrapper> aSourceMap;


  private class DBStatementImpl implements DBStatement {
    
    
    public DBStatementImpl() {
      // Dud that doesn't do anything
    }

    DBStatementImpl(String pSQL, String pErrorMsg) throws SQLException {
      if (DBHelper.this.aConnection==null) {
        if (SHARE_CONNECTIONS) {
          DBHelper.this.aConnection = aDataSource.aConnectionMap.get(DBHelper.this.aKey);
        }
        if (DBHelper.this.aConnection==null) {
          DBHelper.this.aConnection = aDataSource.aDataSource.getConnection();
          DBHelper.this.aConnection.setAutoCommit(false);
          aDataSource.aConnectionMap.put(DBHelper.this.aKey, DBHelper.this.aConnection);
        }
      }
      DBHelper.this.aSQL=aConnection.prepareStatement(pSQL);
      DBHelper.this.aErrorMsg=pErrorMsg;
    }

    @Override
    public DBStatement addParam(int pColumn, String pValue) {
      checkValid();
      if (aSQL!=null) {
        try {
          aSQL.setString(pColumn, pValue);
        } catch (SQLException e) {
          Logger.getGlobal().log(Level.SEVERE, "Failure to create prepared statement", e);
          aSQL=null;
        }
      }
      return this;
    }

    @Override
    public DBStatement addParam(int pColumn, int pValue) {
      checkValid();
      if (aSQL!=null) {
        try {
          aSQL.setInt(pColumn, pValue);
        } catch (SQLException e) {
          Logger.getGlobal().log(Level.SEVERE, "Failure to create prepared statement", e);
          aSQL=null;
        }
      }
      return this;
    }

    @Override
    public DBStatement addParam(int pColumn, long pValue) {
      checkValid();
      if (aSQL!=null) {
        try {
          aSQL.setLong(pColumn, pValue);
        } catch (SQLException e) {
          Logger.getGlobal().log(Level.SEVERE, "Failure to create prepared statement", e);
          aSQL=null;
        }
      }
      return this;
    }

    @Override
    public boolean exec() {
      checkValid();
      DBHelper.this.aValid = false;
      try {
        aSQL.execute();
        return true;
      } catch (SQLException e) {
        logException(aErrorMsg, e);
        try {
          aConnection.rollback();
        } catch (SQLException e1) {
          logException( "Rollback failed", e);
        }
        return false;
      }
    }

    @Override
    public boolean execCommit() {
      boolean result = exec();
      if (result) {
        try {
          aConnection.commit();
        } catch (SQLException e) {
          logException("Commit failed", e);
          try {
            aConnection.rollback();
          } catch (SQLException e1) {
            logException("Rollback failed after commit failed", e1);
          }
          return false;
        }
      }
      return result;
    }
  
  }


  public interface DBQuery extends DBStatement {
    
    @Override
    DBQuery addParam(int pColumn, String pValue);
    @Override
    DBQuery addParam(int pColumn, int pValue);
    @Override
    DBQuery addParam(int pColumn, long pValue);

    boolean execQueryEmpty();

    boolean execQueryNotEmpty();

    /** Execute the query and get the result set. */
    ResultSet execQuery();

    /** Execute the query and return the integer value */
    Integer intQuery();

    /** Execute the query and return the long value */
    Long longQuery();
  }
  
  public class DBQueryImpl extends DBStatementImpl implements DBQuery {

    public DBQueryImpl() {
      super();
    }

    public DBQueryImpl(String pSQL, String pErrorMsg) throws SQLException {
      super(pSQL, pErrorMsg);
    }

    @Override
    public DBQuery addParam(int pColumn, String pValue) {
      return (DBQuery) super.addParam(pColumn, pValue);
    }

    @Override
    public DBQuery addParam(int pColumn, int pValue) {
      return (DBQuery) super.addParam(pColumn, pValue);
    }

    @Override
    public DBQuery addParam(int pColumn, long pValue) {
      return (DBQuery) super.addParam(pColumn, pValue);
    }

    @Override
    public ResultSet execQuery() {
      checkValid();
      DBHelper.this.aValid = false;
      try {
        return aSQL==null ? null : aSQL.executeQuery();
      } catch (SQLException e) {
        logException(aErrorMsg, e);
      }
      return null;
    }
    
    @Override
    public boolean execQueryNotEmpty() {
      ResultSet rs = execQuery();
      try {
        return rs!=null && rs.next();
      } catch (SQLException e) {
        logException("Error processing result set", e);
        return false;
      }
    }
    
    @Override
    public boolean execQueryEmpty() {
      ResultSet rs = execQuery();
      try {
        return rs==null || (! rs.next());
      } catch (SQLException e) {
        logException("Error processing result set", e);
        return true;
      }
    }

    @Override
    public Integer intQuery() {
      try {
        ResultSet rs = getSingleHelper();
        return rs==null ? null : rs.getInt(1);
      } catch (SQLException e) {
        logException("Error processing result set", e);
        return null;
      }
    }

    @Override
    public Long longQuery() {
      try {
        ResultSet rs = getSingleHelper();
        return rs==null ? null : rs.getLong(1);
      } catch (SQLException e) {
        logException("Error processing result set", e);
        return null;
      }
    }

    private ResultSet getSingleHelper() throws SQLException {
      ResultSet rs = execQuery();
      if (rs==null) { return null; }
      if (rs.getMetaData().getColumnCount()!=1) {
        logWarning("The query "+aSQL+ " does not return 1 element");
        return null;
      }
      if (! rs.next()) {
        return null; // No result, that is allowed, no warning
      }
      if (rs.getObject(1)==null) { return null; }
      return rs;
    }
    
  }
  
  public interface DBInsert extends DBStatement {
    @Override
    DBInsert addParam(int pColumn, String pValue);
  
  }


  private class DBInsertImpl extends DBStatementImpl implements DBInsert {


    public DBInsertImpl(String pSQL, String pErrorMsg) throws SQLException {
      super(pSQL, pErrorMsg);
    }

    public DBInsertImpl() {
      // Dud constructor that doesn't do anything
    }

    @Override
    public DBInsert addParam(int pColumn, String pValue) {
      return (DBInsert) super.addParam(pColumn, pValue);
    }
    
  }


  private final DataSourceWrapper aDataSource;

  private DBHelper(DataSourceWrapper pDataSource, Object pKey) {
    aDataSource = pDataSource;
    aKey = pKey!=null ? pKey : new Object();
    aValid=true;
  }
  
  public static DBHelper dbHelper(String pResourceName, Object pKey) {
    if (aSourceMap==null) {
      synchronized (aShareLock) {
        if (aSourceMap==null) {
          aSourceMap = new ConcurrentHashMap<String, DataSourceWrapper>();
        }
      }
    }
    DataSourceWrapper dataSource = aSourceMap.get(pResourceName); 
    if (dataSource==null) {
      try {
        InitialContext initialContext = new InitialContext();
        dataSource = new DataSourceWrapper((DataSource) initialContext.lookup(pResourceName));
        aSourceMap.put(pResourceName, dataSource);
      } catch (NamingException e) {
        logException("Failure to register access permission in database", e);
        return new DBHelper(null, pKey); // Return an empty helper to ensure building doesn't fail stuff
      }
    }
    
    return new DBHelper(dataSource, pKey);
    
  }

  public static void logWarning(String pMsg) {
    Logger.getGlobal().log(Level.WARNING, pMsg);
  }

  public static void logException(final String pMsg, Throwable pE) {
    Logger.getGlobal().log(Level.SEVERE, pMsg, pE);
  }
  
  private void checkValid() {
    if (!aValid) {
      throw new IllegalStateException("DBHelpers can not be reused");
    }
  }
  
  public DBQuery makeQuery(String pSQL) {
    return makeQuery(pSQL, null);
  }

  public DBQuery makeQuery(String pSQL, String pErrorMsg) {
    aValid=true;
    try {
      if (aDataSource!=null) {
        return new DBQueryImpl(pSQL, pErrorMsg);
      }
    } catch (SQLException e) {
      logException("Failure to create prepared statement", e);
    }
    return new DBQueryImpl();
  }

  public DBInsert makeInsert(String pSQL) {
    return makeInsert(pSQL, null);
  }

  public DBInsert makeInsert(String pSQL, String pErrorMsg) {
    aValid=true;
    try {
      if (aDataSource!=null) {
        return new DBInsertImpl(pSQL, pErrorMsg);
      }
    } catch (SQLException e) {
      logException("Failure to create prepared statement", e);
    }
    return new DBInsertImpl();
  }

  public void commit() {
    try {
      aConnection.commit();
    } catch (SQLException e) {
      logException("Failure to commit statement", e);
      try {
        aConnection.rollback();
      } catch (SQLException f) {
        logException("Failure to rollback after failed commit", f);
      }
    }
  }

  public void rollback() {
    try {
      aConnection.rollback();
    } catch (SQLException e) {
      logException("Failure to roll back statement", e);
    }
  }

  public void close() throws SQLException {
    if (aConnection!=null) { aConnection.close(); }
    else if (aDataSource!=null) {
      aConnection = aDataSource.aConnectionMap.get(aKey);
      if (aConnection!=null) { aConnection.close(); }
    }
    aConnection = null;
    if (aDataSource !=null) {
      aDataSource.aConnectionMap.remove(aKey);
    }
  }

}
