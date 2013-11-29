package net.devrieze.chatterbox.server;

import java.security.Principal;
import java.sql.SQLException;

import javax.servlet.ServletRequest;

import net.devrieze.annotations.NotNull;
import net.devrieze.util.db.DBConnection;
import net.devrieze.util.db.DBIterable;


/**
 * Class representing a chat box.
 * @author pdvrieze
 *
 */
public class Box {


  private final ServletRequest aKey;
  private final String aBoxName;
  private long aFirstIndex = -1;
  private long aLastIndex = -1;
  private final int aBoxId;
  private final String aOwner;

  public Box(int pBoxId, String pBoxName, String pOwner, ServletRequest pKey) {
    aBoxId = pBoxId;
    aBoxName = pBoxName;
    aOwner = pOwner;
    aKey = pKey;
  }

  public long getFirstMessageIndex(@NotNull DBConnection connection) throws SQLException {
    if (aFirstIndex<0) { aFirstIndex = ChatboxManager.getFirstIndex(connection, aBoxId, aKey); }
    return aFirstIndex ;
  }

  public long getLastMessageIndex(@NotNull DBConnection connection) throws SQLException {
    if (aLastIndex<0) { aLastIndex = ChatboxManager.getLastIndex(connection, aBoxId, aKey); }
    return aLastIndex ;
  }

  public DBIterable<Message> getMessages(@NotNull DBConnection pConnection) {
    return ChatboxManager.getMessages(pConnection, aBoxId, aKey);
  }

  public DBIterable<Message> getMessages(@NotNull DBConnection pConnection, long start, long end) {
    return ChatboxManager.getMessages(pConnection, aBoxId, start, end, aKey);
  }

  public Message addMessage(@NotNull DBConnection connection, String pMessageBody, Principal pSender) throws SQLException {
    aLastIndex=-1;
    // TODO use transactions
    Message msg = new Message(getNextMsgIndex(connection),pMessageBody, UserManager.getCurrentUserEmail(pSender));
    ChatboxManager.addMessage(connection, aBoxId, msg, aKey);

    return msg;
  }

  private long getNextMsgIndex(@NotNull DBConnection pConnection) throws SQLException {
    return getLastMessageIndex(pConnection)+1;
  }

  public void clear(DBConnection connection) throws SQLException {
    ChatboxManager.clearMessages(connection, aBoxId, aKey);
    connection.commit();
  }

  public CharSequence getName() {
    return aBoxName;
  }

  public String getOwner() {
    return aOwner;
  }

}
