package net.devrieze.chatterbox.server;

import org.jetbrains.annotations.NotNull;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * Class representing a chat box.
 * @author pdvrieze
 *
 */
public class Box {


  private final String aBoxName;
  private long aFirstIndex = -1;
  private long aLastIndex = -1;
  private final int aBoxId;
  private final String aOwner;

  public Box(int pBoxId, String pBoxName, String pOwner) {
    aBoxId = pBoxId;
    aBoxName = pBoxName;
    aOwner = pOwner;
  }

  public long getFirstMessageIndex(@NotNull Connection connection) throws SQLException {
    if (aFirstIndex<0) { aFirstIndex = ChatboxManager.getFirstIndex(connection, aBoxId); }
    return aFirstIndex ;
  }

  public long getLastMessageIndex(@NotNull Connection connection) throws SQLException {
    if (aLastIndex<0) { aLastIndex = ChatboxManager.getLastIndex(connection, aBoxId); }
    return aLastIndex ;
  }

  public PreparedStatement getMessages(@NotNull Connection pConnection) throws SQLException {
    return ChatboxManager.getMessagesStatement(pConnection, aBoxId);
  }

  public PreparedStatement getMessages(@NotNull Connection pConnection, long start, long end) throws SQLException {
    return ChatboxManager.getMessagesStatement(pConnection, aBoxId, start, end);
  }

  public Message addMessage(@NotNull Connection connection, String pMessageBody, Principal pSender) throws SQLException {
    aLastIndex=-1;
    // TODO use transactions
    Message msg = new Message(getNextMsgIndex(connection),pMessageBody, UserManager.getCurrentUserEmail(pSender));
    ChatboxManager.addMessage(connection, aBoxId, msg);

    return msg;
  }

  private long getNextMsgIndex(@NotNull Connection pConnection) throws SQLException {
    return getLastMessageIndex(pConnection)+1;
  }

  public void clear(@NotNull Connection connection) throws SQLException {
    ChatboxManager.clearMessages(connection, aBoxId);
    connection.commit();
  }

  public CharSequence getName() {
    return aBoxName;
  }

  public String getOwner() {
    return aOwner;
  }

}
