package net.devrieze.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;


public class DBHelper {
  
  
  
  
  
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
  public boolean aValid;


  private class DBStatementImpl implements DBStatement {
    
    
    public DBStatementImpl() {
      // Dud that doesn't do anything
    }

    DBStatementImpl(String pSQL, String pErrorMsg) throws SQLException {
      DBHelper.this.aConnection = aDataSource.getConnection();
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
        return aSQL.executeQuery();
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
        return rs==null ? null : rs.getInt(0);
      } catch (SQLException e) {
        logException("Error processing result set", e);
        return null;
      }
    }

    @Override
    public Long longQuery() {
      try {
        ResultSet rs = getSingleHelper();
        return rs==null ? null : rs.getLong(0);
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
      if (rs.getObject(0)==null) { return null; }
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


  private final DataSource aDataSource;

  private DBHelper(DataSource pDataSource) {
    aDataSource = pDataSource;
    aValid=true;
  }
  
  public static DBHelper dbHelper(String pResourceName) {
    InitialContext initialContext;
    try {
      initialContext = new InitialContext();
      return new DBHelper((DataSource) initialContext.lookup(pResourceName));
    } catch (NamingException e) {
      logException("Failure to register access permission in database", e);
      return new DBHelper(null); // Return an empty helper to ensure building doesn't fail stuff
    }
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

}
