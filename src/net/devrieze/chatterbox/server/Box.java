package net.devrieze.chatterbox.server;

import java.security.Principal;

import javax.servlet.ServletRequest;

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

  public long getFirstMessageIndex() {
    if (aFirstIndex<0) { aFirstIndex = ChatboxManager.getFirstIndex(aBoxId, aKey); }
    return aFirstIndex ;
  }
  
  public long getLastMessageIndex() {
    if (aLastIndex<0) { aLastIndex = ChatboxManager.getLastIndex(aBoxId, aKey); }
    return aLastIndex ;
  }
  
  public DBIterable<Message> getMessages() {
    return ChatboxManager.getMessages(aBoxId, aKey);
  }

  public DBIterable<Message> getMessages(long start, long end) {
    return ChatboxManager.getMessages(aBoxId, start, end, aKey);
  }

  public Message addMessage(String pMessageBody, Principal pSender) {
    aLastIndex=-1;
    Message msg = new Message(getNextMsgIndex(),pMessageBody, UserManager.getCurrentUserEmail(pSender));
    ChatboxManager.addMessage(aBoxId, msg, aKey);
    return msg;
  }

  private long getNextMsgIndex() {
    return getLastMessageIndex()+1;
  }

  public void clear() {
    ChatboxManager.clearMessages(aBoxId, aKey);
  }

  public CharSequence getName() {
    return aBoxName;
  }

  public String getOwner() {
    return aOwner;
  }

}
