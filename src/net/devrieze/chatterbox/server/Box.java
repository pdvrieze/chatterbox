package net.devrieze.chatterbox.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;


/**
 * Class representing a chat box.
 * @author pdvrieze
 *
 */
@PersistenceCapable
public class Box {
  
  
  
  private static class AdaptingIterable implements Iterable<Message> {

    private final List<Message> messages;
    private final long start;
    private final long end;

    public AdaptingIterable(List<Message> messages, long start, long end) {
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
      // Not clear why we would need this method, but forwarding is correct
      iterator.remove();
    }

  }

  //@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  @Persistent
  @PrimaryKey
  private String key;
  
  public Box(String key) {
    this.key = key;
  }
  
  @Persistent
  private List<Message> messages;

  public long getFirstMessageIndex() {
    // TODO optimize this by querying
    List<Message> msgs = getMessages();
    return msgs.size()>0 ? msgs.get(0).getIndex() : 0l;
  }
  
  public long getLastMessageIndex() {
    // TODO optimize this by using a query
    List<Message> msgs = getMessages();
    return msgs.size()>0 ? msgs.get(msgs.size()-1).getIndex() : 0l;
  }
  
  public List<Message> getMessages() {
    if (messages==null) { messages = new ArrayList<Message>(); }
    return messages;
  }

  public Iterable<Message> getMessages(long start, long end) {
    return new AdaptingIterable(getMessages(), start, end);
  }
  
  public Iterator<Message> iterator(long start, long end) {
    return new AdaptingIterator(getMessages().iterator(), start, end);
  }
  
  public String getKey() {
    return key;
  }

  public Message addMessage(String message) {
    Message msg = new Message(getNextMsgIndex(),message);
    getMessages().add(msg);
    return msg;
  }

  private long getNextMsgIndex() {
    return getLastMessageIndex()+1;
  }

  public void clear() {
    getMessages().clear();
  }

}
