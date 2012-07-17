package net.devrieze.chatterbox.server;

import java.security.Principal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;

import static net.devrieze.util.DBHelper.*;

import net.devrieze.util.DBHelper;


public class ChatboxManager {
  
  static abstract class ResultSetAdapter<T> implements Iterable<T> {
    ResultSet aResultSet;
    
    ResultSetAdapter(ResultSet pResultSet) {
      aResultSet = pResultSet;
    }
    
    @Override
    public abstract Iterator<T> iterator();
  }
  
  private static class BoxAdapter extends ResultSetAdapter<Box> {
  
    public BoxAdapter(ResultSet pResultSet) {
      super(pResultSet);
    }
  
    @Override
    public Iterator<Box> iterator() {
      return new BoxAdapterIterator(aResultSet);
    }
  
  }

  
  private static class BoxAdapterIterator extends ResultSetAdapterIterator<Box> {
    private int aBoxIdIdx = -1;
    private int aNameIdx = -1;
    private int aOwnerIdx = -1;

    public BoxAdapterIterator(ResultSet pResultSet) {
      super(pResultSet);
    }

    @Override
    protected void doRegisterColumn(int pColumnIndex, String pColumnName) {
      if (COL_BOXID.equals(pColumnName)) {
        aBoxIdIdx=pColumnIndex;
      } else if (COL_BOXNAME.equals(pColumnName)) {
        aNameIdx = pColumnIndex;
      } else if (COL_BOXOWNER.equals(pColumnName)) {
        aOwnerIdx = pColumnIndex;
      }
    }

    @Override
    protected Box doCreateElem(ResultSet pResultSet) throws SQLException {
      int boxId = pResultSet.getInt(aBoxIdIdx);
      String boxName = pResultSet.getString(aNameIdx);
      String owner = pResultSet.getString(aOwnerIdx);
      return new Box(boxId, boxName, owner);
    }
    
  }
  
  
  private static class MessageAdapter extends ResultSetAdapter<Message> {
  
    public MessageAdapter(ResultSet pResultSet) {
      super(pResultSet);
    }
  
    @Override
    public Iterator<Message> iterator() {
      return new MessageAdapterIterator(aResultSet);
    }
  
  }

  static abstract class ResultSetAdapterIterator<T> implements Iterator<T> {

    private final ResultSet aResultSet;
    private boolean aPeeked = false;

    public ResultSetAdapterIterator(ResultSet pResultSet) {
      aResultSet = pResultSet;
      try {
        aResultSet.beforeFirst();
        ResultSetMetaData metadata = aResultSet.getMetaData();
        for (int i=0; i<metadata.getColumnCount();++i) {
          doRegisterColumn(i, metadata.getColumnName(i));
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    protected abstract void doRegisterColumn(int pI, String pColumnName);

    abstract protected T doCreateElem(ResultSet pResultSet) throws SQLException;

    @Override
    public final boolean hasNext() {
      aPeeked =true;
      try {
        return aResultSet.next();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public final T next() {
      try {
        if (! aPeeked) {
          if (!aResultSet.next()) {
            throw new IllegalStateException("Trying to go beyond the last element");
          }
        }
        aPeeked = false;
        
        return doCreateElem(aResultSet);
        
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public final void remove() {
      try {
        aResultSet.deleteRow();
      } catch (SQLFeatureNotSupportedException e) {
        throw new UnsupportedOperationException(e);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  private static class MessageAdapterIterator extends ResultSetAdapterIterator<Message> {
    private int aEpochIdx = -1;
    private int aMsgIndexIdx = -1;
    private int aMsgIdx = -1;
    private int aSenderIdx = -1;

    public MessageAdapterIterator(ResultSet pResultSet) {
      super(pResultSet);
    }

    @Override
    protected void doRegisterColumn(int pColumnIndex, String pColumnName) {
      if (COL_EPOCH.equals(pColumnName)) {
        aEpochIdx=pColumnIndex;
      } else if (COL_MSG_INDEX.equals(pColumnName)) {
        aMsgIndexIdx = pColumnIndex;
      } else if (COL_MESSAGE.equals(pColumnName)) {
        aMsgIdx = pColumnIndex;
      } else if (COL_SENDER.equals(pColumnName)) {
        aSenderIdx = pColumnIndex;
      }
    }

    @Override
    protected Message doCreateElem(ResultSet pResultSet) throws SQLException {
      long index = pResultSet.getLong(aMsgIndexIdx);
      String messageBody = pResultSet.getString(aMsgIdx);
      long epoch = pResultSet.getLong(aEpochIdx);
      String senderEmail = pResultSet.getString(aSenderIdx);
      return new Message(index, messageBody, epoch, senderEmail);
    }
    
  }
  
  private static final String DB_RESOURCE = "jdbc/chatbox";
  
  private static final String TABLE_MESSAGES = "`messages`";
  private static final String TABLE_BOXES = "`boxes`";
  
  private static final String COL_EPOCH = "`epoch`";
  private static final String COL_MESSAGE = "`message`";
  private static final String COL_MSG_INDEX = "`msgIndex`";

  private static final String COL_MSG_ID = "`msgId`";
  private static final String COL_BOXNAME = "`name`";
  private static final String COL_BOXID = "`boxId`";
  private static final String COL_BOXOWNER = "`owner`";

  private static final String COL_SENDER = "`sender`";

  private static final String SQL_INSERT_MESSAGE = "INSERT INTO "+TABLE_MESSAGES+" ("+COL_BOXID+","+COL_MSG_INDEX+","+COL_SENDER+","+COL_MESSAGE+","+COL_EPOCH+") VALUES ( ?, ?, ?, ?, ? )";

  private static final String SQL_CLEAR_BOX = "DELETE FROM "+TABLE_MESSAGES+" WHERE "+COL_BOXID+" = ?";
  
  private static final String SQL_GET_BOX_WITH_NAME = "SELECT * FROM "+TABLE_BOXES+ " WHERE "+COL_BOXNAME+" = ? ORDER BY "+COL_BOXID+" LIMIT 1";
  private static final String SQL_GET_FIRST_INDEX = "SELECT min("+COL_MSG_INDEX+") from "+TABLE_MESSAGES + " WHERE "+COL_BOXID+" = ?";
  private static final String SQL_GET_LAST_INDEX = "SELECT max("+COL_MSG_INDEX+") from "+TABLE_MESSAGES + " WHERE "+COL_BOXID+" = ?";

  private static final String SQL_CREATE_BOXES="CREATE TABLE IF NOT EXISTS " + TABLE_BOXES + " (\n" +
        COL_BOXID + " INTEGER NOT NULL AUTO_INCREMENT,\n"+
  		COL_BOXNAME + " VARCHAR(50),\n" +
        COL_BOXOWNER + " VARCHAR(50),\n" +
  		"PRIMARY KEY (" + COL_BOXID + ") ) engine=innodb;";
  
  private static final String SQL_CREATE_MESSAGES="CREATE TABLE " + TABLE_MESSAGES + " IF NOT EXISTS("+
      COL_MSG_ID + " INTEGER NOT NULL AUTO_INCREMENT," +
      COL_BOXID + " INTEGER NOT NULL," +
      COL_MSG_INDEX + " INTEGER NOT NULL," +
      COL_MESSAGE + " MEDIUMTEXT," +
      COL_EPOCH + " LONG NOT NULL," +
      COL_SENDER + " VARCHAR(50)," +
      "PRIMARY KEY (" + COL_MSG_ID + ")," +
      "FOREIGN KEY (" + COL_BOXID + 
        ") REFERENCES " + TABLE_BOXES + " ( " + COL_BOXID + " )," +
      "UNIQUE ( " + COL_MSG_ID + ", " + COL_MSG_INDEX + " )" +
      " ) ENGINE=InnoDB;";
  private static final String SQL_HAS_TABLES = "SHOW TABLES";

  private static final String SQL_SORT_MESSAGES = " ORDER BY "+COL_MSG_INDEX;
  private static final String SQL_QUERY_MESSAGES_BASE = "SELECT * FROM "+TABLE_MESSAGES+" WHERE "+COL_BOXID+" = ?";
  private static final String SQL_QUERY_ALL_MESSAGES = SQL_QUERY_MESSAGES_BASE + SQL_SORT_MESSAGES;
  private static final String SQL_QUERY_SOME_MESSAGES = SQL_QUERY_MESSAGES_BASE + " AND "+COL_MSG_INDEX+" >= ? AND "+COL_MSG_INDEX+" <= ?" + SQL_SORT_MESSAGES;

  public static Box getBox(String pBoxName) {
    ensureTables();
    Iterator<Box> it = new BoxAdapterIterator(
        dbHelper(DB_RESOURCE)
        .makeQuery(SQL_GET_BOX_WITH_NAME, "Failure verifying chatbox")
        .addParam(1, pBoxName)
        .execQuery()
        );
    if (it.hasNext()) {
      return it.next();
    } else {
      return null;
    }
  }

  private static void ensureTables() {
    DBHelper helper = dbHelper(DB_RESOURCE);
    
    if (helper.makeQuery(SQL_HAS_TABLES).execQueryEmpty()) {
      if (! (helper.makeQuery(SQL_CREATE_BOXES).exec() && 
             helper.makeQuery(SQL_CREATE_MESSAGES).execCommit())) {
        throw new RuntimeException("Tables could not be created");
      }
    }
  }

  public static long getFirstIndex(int pBoxId) {
    return dbHelper(DB_RESOURCE)
        .makeQuery(SQL_GET_FIRST_INDEX, "Could not determine the first message")
        .addParam(1, pBoxId)
        .longQuery();
  }

  public static long getLastIndex(int pBoxId) {
    return dbHelper(DB_RESOURCE)
        .makeQuery(SQL_GET_LAST_INDEX, "Could not determine the last message")
        .addParam(1, pBoxId)
        .longQuery();
  }

  public static void clearMessages(int pBoxId) {
    dbHelper(DB_RESOURCE)
        .makeQuery(SQL_CLEAR_BOX)
        .addParam(1, pBoxId)
        .execCommit();
  }

  public static void addMessage(int pBoxId, Message pMsg) {
    DBHelper helper = dbHelper(DB_RESOURCE);
    boolean retry;
    do {
      long newIndex=1+helper.makeQuery(SQL_GET_LAST_INDEX, "Could not determine the last message")
          .addParam(1, pBoxId)
          .longQuery();
  
      if (! helper.makeQuery(SQL_INSERT_MESSAGE)
          .addParam(1, pBoxId)
          .addParam(2, newIndex)
          .addParam(3, pMsg.getSender())
          .addParam(4, pMsg.getMessage())
          .addParam(5, pMsg.getMsgTime())
          .execCommit()) {
        long idx2 = 1+helper.makeQuery(SQL_GET_LAST_INDEX, "Could not determine the last message")
            .addParam(1, pBoxId)
            .longQuery();
        retry = newIndex!=idx2;
      } else {
        retry = false;
      }
    } while (retry);
  }

  public static Iterable<Message> getMessages(int pBoxId) {
    ResultSet rs = dbHelper(DB_RESOURCE)
        .makeQuery(SQL_QUERY_ALL_MESSAGES)
        .addParam(1, pBoxId)
        .execQuery();
    return new MessageAdapter(rs);
  }

  public static Iterable<Message> getMessageRange(int pBoxId, long pStart, long pEnd) {
    ResultSet rs = dbHelper(DB_RESOURCE)
        .makeQuery(SQL_QUERY_SOME_MESSAGES)
        .addParam(1, pBoxId)
        .addParam(2, pStart)
        .addParam(3, pEnd)
        .execQuery();
    return new MessageAdapter(rs);
  }

  public static Iterable<Box> getBoxes() {
    ResultSet rs = dbHelper(DB_RESOURCE)
        .makeQuery("SELECT * FROM "+TABLE_BOXES)
        .execQuery();
    return new BoxAdapter(rs);
  }

  public static boolean isAdmin(Box pBox, Principal pUserPrincipal) {
    String owner = pBox.getOwner();
    return owner!=null && owner.equals(pUserPrincipal.getName());
  }

}
