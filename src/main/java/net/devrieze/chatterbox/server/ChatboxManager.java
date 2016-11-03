package net.devrieze.chatterbox.server;

import org.jetbrains.annotations.NotNull;

import java.security.Principal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ChatboxManager {

  public static class BoxAdapter {
    private int aBoxIdIdx = -1;
    private int aNameIdx = -1;
    private int aOwnerIdx = -1;

    public BoxAdapter(final ResultSetMetaData metaData) throws SQLException {
      for(int i=metaData.getColumnCount(); i>0; --i) {
        String pColumnName = metaData.getColumnName(i);
        if (unquote(COL_BOXID).equals(pColumnName)) {
          aBoxIdIdx= i;
        } else if (unquote(COL_BOXNAME).equals(pColumnName)) {
          aNameIdx = i;
        } else if (unquote(COL_BOXOWNER).equals(pColumnName)) {
          aOwnerIdx = i;
        }
      }
    }


    public static Box toBox(ResultSet resultSet) throws SQLException {
      return new BoxAdapter(resultSet.getMetaData()).getBox(resultSet);
    }

    public Box getBox(ResultSet resultSet) throws SQLException {
      if (aBoxIdIdx<1 || aNameIdx < 1 || aOwnerIdx<1) {
        throw new IllegalArgumentException("The given query does not return the expected columns");
      }
      int boxId = resultSet.getInt(aBoxIdIdx);
      String boxName = resultSet.getString(aNameIdx);
      String owner = resultSet.getString(aOwnerIdx);
      return new Box(boxId, boxName, owner);
    }
  }


  public static class MessageAdapter {

    private int aEpochIdx = -1;
    private int aMsgIndexIdx = -1;
    private int aMsgIdx = -1;
    private int aSenderIdx = -1;

    public MessageAdapter(ResultSetMetaData metaData) throws SQLException {
      for(int i=metaData.getColumnCount(); i>0; --i) {
        String pColumnName = metaData.getColumnName(i);
        if (unquote(COL_EPOCH).equals(pColumnName)) {
          aEpochIdx=i;
        } else if (unquote(COL_MSG_INDEX).equals(pColumnName)) {
          aMsgIndexIdx = i;
        } else if (unquote(COL_MESSAGE).equals(pColumnName)) {
          aMsgIdx = i;
        } else if (unquote(COL_SENDER).equals(pColumnName)) {
          aSenderIdx = i;
        }
      }
    }

    protected static Message toMessage(ResultSet resultSet) throws SQLException {
      return new MessageAdapter(resultSet.getMetaData()).getMessage(resultSet);
    }

    protected Message getMessage(ResultSet resultSet) throws SQLException {
      if (aEpochIdx<1 || aMsgIndexIdx < 1 || aMsgIdx<1 || aSenderIdx < 1) {
        throw new IllegalArgumentException("The given query does not return the expected columns");
      }
      long index = resultSet.getLong(aMsgIndexIdx);
      String messageBody = resultSet.getString(aMsgIdx);
      long epoch = resultSet.getLong(aEpochIdx);
      String senderEmail = resultSet.getString(aSenderIdx);
      return new Message(index, messageBody, epoch, senderEmail);
    }

  }

  public static final String CHATBOX_DB_RESOURCE = "java:/comp/env/jdbc/chatbox";

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

  @NotNull
  private static final String SQL_INSERT_BOX = "INSERT INTO "+TABLE_BOXES + " ( "+COL_BOXNAME+", "+COL_BOXOWNER+" ) VALUES ( ?, ? )";

  private static final String COL_SENDER = "`sender`";

  private static final String COL_TOKENNAME = "`name`";

  @NotNull
  private static final String SQL_REMOVE_TOKEN = "DELETE FROM "+TABLE_TOKENS+" WHERE "+COL_TOKENNAME+" = ?";

  @NotNull
  private static final String SQL_GET_TOKENS = "SELECT "+COL_TOKENNAME+" FROM "+TABLE_TOKENS;

  private static final String COL_TOKENID = "`tokenId`";

  @NotNull
  private static final String SQL_INSERT_MESSAGE = "INSERT INTO "+TABLE_MESSAGES+" ("+COL_BOXID+","+COL_MSG_INDEX+","+COL_SENDER+","+COL_MESSAGE+","+COL_EPOCH+") VALUES ( ?, ?, ?, ?, ? )";

  @NotNull
  private static final String SQL_CLEAR_BOX = "DELETE FROM "+TABLE_MESSAGES+" WHERE "+COL_BOXID+" = ?";

  @NotNull
  private static final String SQL_GET_BOX_WITH_NAME = "SELECT * FROM "+TABLE_BOXES+ " WHERE "+COL_BOXNAME+" = ? ORDER BY "+COL_BOXID+" LIMIT 1";

  @NotNull
  private static final String SQL_GET_FIRST_INDEX = "SELECT min("+COL_MSG_INDEX+") from "+TABLE_MESSAGES + " WHERE "+COL_BOXID+" = ?";

  @NotNull
  private static final String SQL_GET_LAST_INDEX = "SELECT max("+COL_MSG_INDEX+") from "+TABLE_MESSAGES + " WHERE "+COL_BOXID+" = ?";

  @NotNull
  private static final String SQL_CREATE_TOKENS="CREATE TABLE IF NOT EXISTS " + TABLE_TOKENS + " (\n" +
        COL_TOKENID + " INTEGER NOT NULL AUTO_INCREMENT,\n"+
        COL_TOKENNAME + " VARCHAR(50),\n" +
        "UNIQUE ( "+COL_TOKENNAME+" ),\n" +
        "PRIMARY KEY (" + COL_TOKENID + ") ) engine=innodb;";

  @NotNull
  private static final String SQL_CREATE_BOXES="CREATE TABLE IF NOT EXISTS " + TABLE_BOXES + " (\n" +
        COL_BOXID + " INTEGER NOT NULL AUTO_INCREMENT,\n"+
  		COL_BOXNAME + " VARCHAR(50),\n" +
        COL_BOXOWNER + " VARCHAR(50),\n" +
  		"PRIMARY KEY (" + COL_BOXID + ") ) engine=innodb;";

  @NotNull
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

  @NotNull
  private static final String SQL_HAS_TABLES = "SHOW TABLES";

  @NotNull
  private static final String SQL_SORT_MESSAGES = " ORDER BY "+COL_MSG_INDEX;

  @NotNull
  private static final String SQL_QUERY_MESSAGES_BASE = "SELECT * FROM "+TABLE_MESSAGES+" WHERE "+COL_BOXID+" = ?";

  @NotNull
  private static final String SQL_QUERY_ALL_MESSAGES = SQL_QUERY_MESSAGES_BASE + SQL_SORT_MESSAGES;

  @NotNull
  private static final String SQL_QUERY_SOME_MESSAGES = SQL_QUERY_MESSAGES_BASE + " AND "+COL_MSG_INDEX+" >= ? AND "+COL_MSG_INDEX+" <= ?" + SQL_SORT_MESSAGES;

  private static boolean _tables_ensured = false;

  public static Box getBox(@NotNull Connection pConnection, String pBoxName) throws SQLException {
    ensureTables(pConnection);

    try (final PreparedStatement statement = pConnection.prepareStatement(SQL_GET_BOX_WITH_NAME)) {
      statement.setString(1, pBoxName);
      try (ResultSet results = statement.executeQuery()) {
        if (results.next()) {
          return BoxAdapter.toBox(results);
        }
        return null;
      }
    }
  }

  static void ensureTables(Connection connection) throws SQLException {
    if (_tables_ensured ) { return; }
    _tables_ensured = true;

    Savepoint savePoint = connection.setSavepoint();
    try {
      Set<String> tables;
      try (final PreparedStatement query = connection.prepareStatement(SQL_HAS_TABLES)) {
        try (final ResultSet resultSet = query.executeQuery()) {
          tables = new TreeSet<String>();
          while (resultSet.next()) {
            tables.add(resultSet.getString(1));
          }
        }
      }

      boolean success = true;
      boolean changed = false;
      if (!tables.contains(unquote(TABLE_BOXES))) {
        try (PreparedStatement query = connection.prepareStatement(SQL_CREATE_BOXES)) {
          success = query.execute();
          changed = changed || success;
        }
      }
      if (success && (!tables.contains(unquote(TABLE_MESSAGES)))) {
        try (PreparedStatement query = connection.prepareStatement(SQL_CREATE_MESSAGES)) {
          success = query.execute();
          changed = changed || success;
        }
      }
      if (success && (!tables.contains(unquote(TABLE_TOKENS)))) {
        try (PreparedStatement query = connection.prepareStatement(SQL_CREATE_TOKENS)) {
          success = query.execute();
          changed = changed || success;
        }
      }
      if (success) {
        if (changed) {
          connection.commit();
        }
      } else {
        throw new SQLException("Tables could not be created");
      }
    } catch (SQLException e) {
      connection.rollback(savePoint);
      throw e;
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

  public static long getFirstIndex(@NotNull Connection pConnection, int pBoxId) throws SQLException {
    return doSimpleLongQuery(pConnection, SQL_GET_FIRST_INDEX, pBoxId);
  }

  private static long doSimpleLongQuery(final @NotNull Connection pConnection, final String sql, final int param) throws
          SQLException {
    try (PreparedStatement statement = pConnection.prepareStatement(sql)) {
      statement.setInt(1, param);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getLong(1);
        }
        return Long.MIN_VALUE;
      }
    }
  }

  public static long getLastIndex(@NotNull Connection pConnection, int pBoxId) throws SQLException {
    return doSimpleLongQuery(pConnection, SQL_GET_LAST_INDEX, pBoxId);
  }

  public static void clearMessages(@NotNull Connection pConnection, int pBoxId) throws SQLException {
    try (PreparedStatement statement = pConnection.prepareStatement(SQL_CLEAR_BOX)) {
      statement.setInt(1, pBoxId);
      if (!statement.execute()) {
        throw new SQLException("Failure to clear the messages");
      }
    }
  }

  public static void addMessage(@NotNull Connection pConnection, int pBoxId, Message pMsg) throws SQLException {
    boolean retry = true;
    do {
      Savepoint savePoint = pConnection.setSavepoint();
      try {
        final long oldIndex = doSimpleLongQuery(pConnection, SQL_GET_LAST_INDEX, pBoxId);
        long newIndex = oldIndex == Long.MIN_VALUE ? 0 : 1 + oldIndex;

        int changeCount;
        try (PreparedStatement statement = pConnection.prepareStatement(SQL_INSERT_MESSAGE)) {
          statement.setInt(1, pBoxId);
          statement.setLong(2, newIndex);
          statement.setString(3, pMsg.getSender());
          statement.setString(4, pMsg.getMessageBody());
          statement.setLong(5, pMsg.getMsgTime());
          changeCount = statement.executeUpdate();
        }
        if (changeCount > 0) {
          long idx2 = doSimpleLongQuery(pConnection, SQL_GET_LAST_INDEX, pBoxId);
          retry = newIndex != idx2;
        } else {
          retry = false;
        }
        pConnection.commit();
      } catch (SQLException e) {
        try {
          pConnection.rollback(savePoint);
        } catch (SQLException f) {
          e.addSuppressed(f);
        }
      }
    } while (retry);
  }

  public static PreparedStatement getMessagesStatement(@NotNull Connection pConnection, int pBoxId) throws SQLException {
    final PreparedStatement statement = pConnection.prepareStatement(SQL_QUERY_ALL_MESSAGES);
    statement.setInt(1, pBoxId);
    return statement;
  }

  public static PreparedStatement getMessagesStatement(@NotNull Connection pConnection, int pBoxId, long pStart, long pEnd) throws
          SQLException {
    final PreparedStatement statement = pConnection.prepareStatement(SQL_QUERY_SOME_MESSAGES);
        statement.setInt(1, pBoxId);
        statement.setLong(2, pStart);
        statement.setLong(3, pEnd);
    return statement;
  }

  @SuppressWarnings("resource")
  public static PreparedStatement getBoxesStatement(@NotNull Connection pConnection) throws SQLException {
    return pConnection.prepareStatement("SELECT * FROM "+TABLE_BOXES);
  }

  public static boolean isAdmin(Box pBox, Principal pUserPrincipal) {
    String owner = pBox.getOwner();
    return owner!=null && owner.equals(pUserPrincipal.getName());
  }

  public static boolean isValidToken(@NotNull Connection pConnection, String pToken) throws SQLException {
    try (PreparedStatement statement = pConnection.prepareStatement("SELECT * FROM " + TABLE_TOKENS + " WHERE " + COL_TOKENNAME + "=?")) {
      statement.setString(1, pToken);
      try (ResultSet result = statement.executeQuery()) {
        return result.next();
      }
    }
  }

  public static boolean isSystemAdmin(Principal pUserPrincipal) {
    return "pdvrieze".equals(pUserPrincipal.getName());
    // TODO Don't hard code this, but look it up.
  }

  @SuppressWarnings("resource")
  public static Iterable<String> getAuthTokens(@NotNull Connection pConnection) throws SQLException {
    try (final PreparedStatement statement = pConnection.prepareStatement(SQL_GET_TOKENS)) {
      try (ResultSet results = statement.executeQuery()) {
        List<String> result = new ArrayList<>();
        while (results.next()) {
          result.add(results.getString(1));
        }
        return result;
      }
    }
  }

  public static Box createBox(@NotNull Connection pConnection, String pBoxName, String pBoxOwner) throws SQLException {
    try (PreparedStatement statement = pConnection.prepareStatement(SQL_INSERT_BOX)) {
      statement.setString(1, pBoxName);
      statement.setString(2, pBoxOwner);
      if(statement.executeUpdate()>0) {
        pConnection.commit();
      }
    }
    return getBox(pConnection, pBoxName);
  }

  public static void removeAuthToken(@NotNull Connection pConnection, String pToken) throws SQLException {
    try (PreparedStatement statement = pConnection.prepareStatement(SQL_REMOVE_TOKEN)) {
      statement.setString(1, pToken);
      if (statement.executeUpdate() > 0) {
        pConnection.commit();
      }
    }
  }

  public static void destroy() {
    // Does nothing
  }

}
