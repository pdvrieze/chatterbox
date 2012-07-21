package net.devrieze.chatterbox.server;

import java.security.Principal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletRequest;

import static net.devrieze.util.DBHelper.*;

import net.devrieze.util.DBHelper;
import net.devrieze.util.ResultSetAdapter;
import net.devrieze.util.ResultSetAdapter.ResultSetAdapterIterator;
import net.devrieze.util.StringAdapter;


public class ChatboxManager {
  
  private static class BoxAdapter extends ResultSetAdapter<Box> {
  
    private ServletRequest aKey;

    public BoxAdapter(ResultSet pResultSet, ServletRequest pKey) {
      super(pResultSet);
      aKey = pKey;
    }
  
    @Override
    public Iterator<Box> iterator() {
      return new BoxAdapterIterator(aResultSet, aKey);
    }
  
  }

  
  private static class BoxAdapterIterator extends ResultSetAdapterIterator<Box> {
    private int aBoxIdIdx = -1;
    private int aNameIdx = -1;
    private int aOwnerIdx = -1;
    private final ServletRequest aKey;

    public BoxAdapterIterator(ResultSet pResultSet, ServletRequest pKey) {
      super(pResultSet);
      aKey = pKey;
    }

    @Override
    protected void doRegisterColumn(int pColumnIndex, String pColumnName) {
      if (unquote(COL_BOXID).equals(pColumnName)) {
        aBoxIdIdx=pColumnIndex;
      } else if (unquote(COL_BOXNAME).equals(pColumnName)) {
        aNameIdx = pColumnIndex;
      } else if (unquote(COL_BOXOWNER).equals(pColumnName)) {
        aOwnerIdx = pColumnIndex;
      }
    }

    @Override
    protected Box doCreateElem(ResultSet pResultSet) throws SQLException {
      if (aBoxIdIdx<1 || aNameIdx < 1 || aOwnerIdx<1) {
        throw new IllegalArgumentException("The given query does not return the expected columns");
      }
      int boxId = pResultSet.getInt(aBoxIdIdx);
      String boxName = pResultSet.getString(aNameIdx);
      String owner = pResultSet.getString(aOwnerIdx);
      return new Box(boxId, boxName, owner, aKey);
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
      if (unquote(COL_EPOCH).equals(pColumnName)) {
        aEpochIdx=pColumnIndex;
      } else if (unquote(COL_MSG_INDEX).equals(pColumnName)) {
        aMsgIndexIdx = pColumnIndex;
      } else if (unquote(COL_MESSAGE).equals(pColumnName)) {
        aMsgIdx = pColumnIndex;
      } else if (unquote(COL_SENDER).equals(pColumnName)) {
        aSenderIdx = pColumnIndex;
      }
    }

    @Override
    protected Message doCreateElem(ResultSet pResultSet) throws SQLException {
      if (aEpochIdx<1 || aMsgIndexIdx < 1 || aMsgIdx<1 || aSenderIdx < 1) {
        throw new IllegalArgumentException("The given query does not return the expected columns");
      }
      long index = pResultSet.getLong(aMsgIndexIdx);
      String messageBody = pResultSet.getString(aMsgIdx);
      long epoch = pResultSet.getLong(aEpochIdx);
      String senderEmail = pResultSet.getString(aSenderIdx);
      return new Message(index, messageBody, epoch, senderEmail);
    }
    
  }
  
  private static final String DB_RESOURCE = "java:/comp/env/jdbc/chatbox";
  
  private static final String TABLE_MESSAGES = "`messages`";
  private static final String TABLE_BOXES = "`boxes`";
  
  private static final String TABLE_TOKENS = "`tokens`";

  private static final String COL_EPOCH = "`epoch`";
  private static final String COL_MESSAGE = "`message`";
  private static final String COL_MSG_INDEX = "`msgIndex`";

  private static final String COL_MSG_ID = "`msgId`";
  private static final String COL_BOXNAME = "`name`";
  private static final String COL_BOXID = "`boxId`";
  private static final String COL_BOXOWNER = "`owner`";

  private static final String SQL_INSERT_BOX = "INSERT INTO "+TABLE_BOXES + " ( "+COL_BOXNAME+", "+COL_BOXOWNER+" ) VALUES ( ?, ? )";

  private static final String COL_SENDER = "`sender`";

  private static final String COL_TOKENNAME = "`name`";

  private static final String SQL_REMOVE_TOKEN = "DELETE FROM "+TABLE_TOKENS+" WHERE "+COL_TOKENNAME+" = ?";

  private static final String SQL_GET_TOKENS = "SELECT "+COL_TOKENNAME+" FROM "+TABLE_TOKENS;

  private static final String COL_TOKENID = "`tokenId`";

  private static final String SQL_INSERT_MESSAGE = "INSERT INTO "+TABLE_MESSAGES+" ("+COL_BOXID+","+COL_MSG_INDEX+","+COL_SENDER+","+COL_MESSAGE+","+COL_EPOCH+") VALUES ( ?, ?, ?, ?, ? )";

  private static final String SQL_CLEAR_BOX = "DELETE FROM "+TABLE_MESSAGES+" WHERE "+COL_BOXID+" = ?";
  
  private static final String SQL_GET_BOX_WITH_NAME = "SELECT * FROM "+TABLE_BOXES+ " WHERE "+COL_BOXNAME+" = ? ORDER BY "+COL_BOXID+" LIMIT 1";
  private static final String SQL_GET_FIRST_INDEX = "SELECT min("+COL_MSG_INDEX+") from "+TABLE_MESSAGES + " WHERE "+COL_BOXID+" = ?";
  private static final String SQL_GET_LAST_INDEX = "SELECT max("+COL_MSG_INDEX+") from "+TABLE_MESSAGES + " WHERE "+COL_BOXID+" = ?";

  private static final String SQL_CREATE_TOKENS="CREATE TABLE IF NOT EXISTS " + TABLE_TOKENS + " (\n" +
        COL_TOKENID + " INTEGER NOT NULL AUTO_INCREMENT,\n"+
        COL_TOKENNAME + " VARCHAR(50),\n" +
        "UNIQUE ( "+COL_TOKENNAME+" ),\n" +
        "PRIMARY KEY (" + COL_TOKENID + ") ) engine=innodb;";

  private static final String SQL_CREATE_BOXES="CREATE TABLE IF NOT EXISTS " + TABLE_BOXES + " (\n" +
        COL_BOXID + " INTEGER NOT NULL AUTO_INCREMENT,\n"+
  		COL_BOXNAME + " VARCHAR(50),\n" +
        COL_BOXOWNER + " VARCHAR(50),\n" +
  		"PRIMARY KEY (" + COL_BOXID + ") ) engine=innodb;";
  
  private static final String SQL_CREATE_MESSAGES="CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGES + " ("+
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

  private static boolean _tables_ensured = false;

  public static Box getBox(String pBoxName, ServletRequest pKey) {
    ensureTables(pKey);
    Iterator<Box> it = new BoxAdapterIterator(
        dbHelper(DB_RESOURCE, pKey)
        .makeQuery(SQL_GET_BOX_WITH_NAME, "Failure verifying chatbox")
        .addParam(1, pBoxName)
        .execQuery()
        , pKey);
    if (it.hasNext()) {
      return it.next();
    } else {
      return null;
    }
  }

  static void ensureTables(Object pKey) {
    if (_tables_ensured ) { return; }
    _tables_ensured = true;
    
    
    DBHelper helper = dbHelper(DB_RESOURCE, pKey);
    
    Set<String> tables = new TreeSet<String>();
    for (String table: new StringAdapter(helper.makeQuery(SQL_HAS_TABLES).execQuery())) {
      tables.add(table);
    }
    boolean success = true;
    boolean changed = false;
    if (! tables.contains(unquote(TABLE_BOXES))) {
      success = helper.makeQuery(SQL_CREATE_BOXES).exec();
      changed = changed || success;
    }
    if (success && (! tables.contains(unquote(TABLE_MESSAGES)))) {
      success = helper.makeQuery(SQL_CREATE_MESSAGES).exec();
      changed = changed || success;
    }
    if (success && (! tables.contains(unquote(TABLE_TOKENS)))) {
      success = helper.makeQuery(SQL_CREATE_TOKENS).exec();
      changed = changed || success;
    }
    if (success) {
      if (changed) {
        helper.commit();
      }
    } else {
      helper.rollback();
      throw new RuntimeException("Tables could not be created");
    }
  }

  private static String unquote(String pQuoted) {
    String result = pQuoted;
    char c=result.charAt(0);
    if (c=='"' || c=='\'' || c=='`') {
      result = result.substring(1);
    } else { c=0;} 
    
    char d=result.charAt(result.length()-1);
    if (c==d || (c==0 && (d=='"' || d=='\'' || d=='`') )) {
      result = result.substring(0, result.length()-1);
    }
    return result;
  }

  public static long getFirstIndex(int pBoxId, ServletRequest pKey) {
    final Long result = dbHelper(DB_RESOURCE, pKey)
        .makeQuery(SQL_GET_FIRST_INDEX, "Could not determine the first message")
        .addParam(1, pBoxId)
        .longQuery();
    return result == null ? 0 : result.longValue();
  }

  public static long getLastIndex(int pBoxId, ServletRequest pKey) {
    final Long result = dbHelper(DB_RESOURCE, pKey)
        .makeQuery(SQL_GET_LAST_INDEX, "Could not determine the last message")
        .addParam(1, pBoxId)
        .longQuery();
    return result == null ? 0 : result.longValue();
  }

  public static void clearMessages(int pBoxId, ServletRequest pKey) {
    dbHelper(DB_RESOURCE, pKey)
        .makeQuery(SQL_CLEAR_BOX)
        .addParam(1, pBoxId)
        .execCommit();
  }

  public static void addMessage(int pBoxId, Message pMsg, ServletRequest pKey) {
    DBHelper helper = dbHelper(DB_RESOURCE, pKey);
    boolean retry;
    do {
      final Long oldIndex = helper.makeQuery(SQL_GET_LAST_INDEX, "Could not determine the last message")
          .addParam(1, pBoxId)
          .longQuery();
      long newIndex=oldIndex==null ? 0 : 1+oldIndex.longValue();
  
      if (! helper.makeQuery(SQL_INSERT_MESSAGE)
          .addParam(1, pBoxId)
          .addParam(2, newIndex)
          .addParam(3, pMsg.getSender())
          .addParam(4, pMsg.getMessageBody())
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

  public static Iterable<Message> getMessages(int pBoxId, ServletRequest pKey) {
    ResultSet rs = dbHelper(DB_RESOURCE, pKey)
        .makeQuery(SQL_QUERY_ALL_MESSAGES)
        .addParam(1, pBoxId)
        .execQuery();
    return new MessageAdapter(rs);
  }

  public static Iterable<Message> getMessages(int pBoxId, long pStart, long pEnd, ServletRequest pKey) {
    ResultSet rs = dbHelper(DB_RESOURCE, pKey)
        .makeQuery(SQL_QUERY_SOME_MESSAGES)
        .addParam(1, pBoxId)
        .addParam(2, pStart)
        .addParam(3, pEnd)
        .execQuery();
    return new MessageAdapter(rs);
  }

  public static Iterable<Box> getBoxes(ServletRequest pKey) {
    ResultSet rs = dbHelper(DB_RESOURCE, pKey)
        .makeQuery("SELECT * FROM "+TABLE_BOXES)
        .execQuery();
    return new BoxAdapter(rs, pKey);
  }

  public static boolean isAdmin(Box pBox, Principal pUserPrincipal) {
    String owner = pBox.getOwner();
    return owner!=null && owner.equals(pUserPrincipal.getName());
  }

  public static boolean isValidToken(String pToken, ServletRequest pKey) {
    return dbHelper(DB_RESOURCE, pKey)
        .makeQuery("SELECT * FROM "+TABLE_TOKENS+" WHERE "+COL_TOKENNAME+"=?")
        .addParam(1, pToken)
        .execQueryNotEmpty();
  }

  public static boolean isSystemAdmin(Principal pUserPrincipal) {
    return "pdvrieze".equals(pUserPrincipal.getName());
    // TODO Don't hard code this, but look it up.
  }

  public static Iterable<String> getAuthTokens(ServletRequest pKey) {
    return new StringAdapter(dbHelper(DB_RESOURCE, pKey)
        .makeQuery(SQL_GET_TOKENS)
        .execQuery());
  }

  public static Box createBox(String pBoxName, String pBoxOwner, ServletRequest pKey) {
    dbHelper(DB_RESOURCE, pKey)
        .makeInsert(SQL_INSERT_BOX)
        .addParam(1, pBoxName)
        .addParam(2, pBoxOwner)
        .execCommit();
    return getBox(pBoxName, pKey);
  }

  public static void removeAuthToken(String pToken, ServletRequest pKey) {
    dbHelper(DB_RESOURCE, pKey)
        .makeQuery(SQL_REMOVE_TOKEN)
        .addParam(1, pToken)
        .execCommit();
  }

}
