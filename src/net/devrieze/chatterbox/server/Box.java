package net.devrieze.chatterbox.server;

import java.security.Principal;
import java.util.Iterator;


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

  public long getFirstMessageIndex() {
    if (aFirstIndex<0) { aFirstIndex = ChatboxManager.getFirstIndex(aBoxId); }
    return aFirstIndex ;
  }
  
  public long getLastMessageIndex() {
    if (aLastIndex<0) { aLastIndex = ChatboxManager.getLastIndex(aBoxId); }
    return aLastIndex ;
  }
  
  public Iterable<Message> getMessages() {
    return ChatboxManager.getMessages(aBoxId);
  }

  public Iterable<Message> getMessages(long start, long end) {
    return ChatboxManager.getMessages(aBoxId, start, end);
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
    ChatboxManager.addMessage(aBoxId, msg);
    return msg;
  }

  private long getNextMsgIndex() {
    return getLastMessageIndex()+1;
  }

  public void clear() {
    ChatboxManager.clearMessages(aBoxId);
  }

  public CharSequence getName() {
    return aBoxName;
  }

  public String getOwner() {
    return aOwner;
  }

}
