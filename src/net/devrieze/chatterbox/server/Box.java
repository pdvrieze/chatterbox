package net.devrieze.chatterbox.server;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Class representing a chat box.
 * @author pdvrieze
 *
 */
public class Box {
  
  
  
  private final String aBoxName;
  private long aFirstIndex = -1;
  private long aLastIndex = -1;

  private static class AdaptingIterable implements Iterable<Message> {

    private final Iterable<Message> messages;
    private final long start;
    private final long end;

    public AdaptingIterable(Iterable<Message> messages, long start, long end) {
      this.start = start;
      this.end = end;
      this.messages = messages;
    }

    @Override
    public Iterator<Message> iterator() {
      return new AdaptingIterator(messages.iterator(), start, end);
    }

  }

  private static class AdaptingIterator implements Iterator<Message> {

    private final Iterator<Message> iterator;
    private final long start;
    private final long end;
    private Message buffer = null;

    public AdaptingIterator(Iterator<Message> iterator, long start, long end) {
      this.iterator = iterator;
      this.start = start;
      this.end = end;
    }

    @Override
    public boolean hasNext() {
      if (buffer!=null) { return true; }
      while(iterator.hasNext()) {
        buffer = iterator.next();
        long idx = buffer.getIndex();
        if (start<=idx && end>=idx) {
          return true;
        }
      }
      buffer = null;
      return false;
    }

    @Override
    public Message next() {
      if (!hasNext()) {
        throw new NoSuchElementException("There is no next message within range");
      }
      assert buffer!=null; // Buffer should not be null if hasNext is true;
      Message result = buffer;
      buffer = null;
      return result;
    }

    @Override
    public void remove() {
      // TODO potentially buggy if hasNext has been called. Check for that and throw an exception
      // Not clear why we would need this method, but forwarding is correct
      iterator.remove();
    }

  }

  //@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  public Box(String pBoxName) {
    aBoxName = pBoxName;
  }

  public long getFirstMessageIndex() {
    if (aFirstIndex<0) { aFirstIndex = ChatboxManager.getFirstIndex(aBoxName); }
    return aFirstIndex ;
  }
  
  public long getLastMessageIndex() {
    if (aLastIndex<0) { aLastIndex = ChatboxManager.getLastIndex(aBoxName); }
    return aLastIndex ;
  }
  
  public Iterable<Message> getMessages() {
    return ChatboxManager.getMessages(aBoxName);
  }

  public Iterable<Message> getMessages(long start, long end) {
    return new AdaptingIterable(getMessages(), start, end);
  }
  
  public Iterator<Message> iterator(long start, long end) {
    return new AdaptingIterator(getMessages().iterator(), start, end);
  }

  public Message addMessage(String messageBody) {
    Message msg = new Message(getNextMsgIndex(),messageBody);
    ChatboxManager.addMessage(aBoxName, msg);
    return msg;
  }

  private long getNextMsgIndex() {
    return getLastMessageIndex()+1;
  }

  public void clear() {
    ChatboxManager.clearMessages(aBoxName);
  }

}
