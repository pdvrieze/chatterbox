package net.devrieze.chatterbox.server;

import java.security.Principal;
import java.util.Iterator;

import javax.servlet.ServletRequest;


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
  
  public Iterable<Message> getMessages() {
    return ChatboxManager.getMessages(aBoxId, aKey);
  }

  public Iterable<Message> getMessages(long start, long end) {
    return ChatboxManager.getMessages(aBoxId, start, end, aKey);
  }
  
  /**
   * 
   * @deprecated Just a wrapper over {@link #getMessages(long, long)}
   */
  @Deprecated
  public Iterator<Message> iterator(long start, long end) {
    return getMessages(start, end).iterator();
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
