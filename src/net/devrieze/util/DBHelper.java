package net.devrieze.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;


public class DBHelper {
  
  
  
  
  
  public interface DBStatement {

    DBStatement addParam(int pColumn, String pValue);
    
    void execCommit();

  }


  public String aErrorMsg;
  public PreparedStatement aSQL;
  private Connection aConnection;
  public boolean aValid;


  private class DBStatementImpl implements DBStatement {

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
    public void execCommit() {
      checkValid();
      try {
        aSQL.execute();
        aConnection.commit();
      } catch (SQLException e) {
        Logger.getGlobal().log(Level.SEVERE, aErrorMsg, e);
        try {
          aConnection.rollback();
        } catch (SQLException e1) {
          Logger.getGlobal().log(Level.SEVERE, "Rollback failed", e);
        }
      }
      DBHelper.this.aValid = false;
    }
  
  }


  public interface DBInsert extends DBStatement {
    @Override
    DBInsert addParam(int pColumn, String pValue);
  
  }


  private class DBInsertImpl extends DBStatementImpl implements DBInsert {


    public DBInsertImpl(String pSQL, String pErrorMsg) throws SQLException {
      DBHelper.this.aConnection = aDataSource.getConnection();
      DBHelper.this.aSQL=aConnection.prepareStatement(pSQL);
      DBHelper.this.aErrorMsg=pErrorMsg;
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
      Logger.getGlobal().log(Level.SEVERE, "Failure to register access permission in database", e);
      return new DBHelper(null); // Return an empty helper to ensure building doesn't fail stuff
    }
  }

  public DBInsert makeInsert(String pSQL) {
    return makeInsert(pSQL, null);
  }
  
  
  private void checkValid() {
    if (!aValid) {
      throw new IllegalStateException("DBHelpers can not be reused");
    }
  }

  public DBInsert makeInsert(String pSQL, String pErrorMsg) {
    aValid=true;
    try {
      if (aDataSource!=null) {
        return new DBInsertImpl(pSQL, pErrorMsg);
      }
    } catch (SQLException e) {
      Logger.getGlobal().log(Level.SEVERE, "Failure to create prepared statement", e);
    }
    return new DBInsertImpl();
  }

}
