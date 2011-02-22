package net.devrieze.chatterbox.server;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

@PersistenceCapable
public class Message {
  
  @SuppressWarnings("unused")
  @Persistent
  @PrimaryKey
  private Key key;
  
  @Persistent
  private Long index;
  
  @Persistent
  private String message;
  
  @Persistent
  private Long msgTime;

  public Message(long index, String message) {
    this.index = index;
    this.key = KeyFactory.createKey(Message.class.getSimpleName(), index);
    this.message = message;
    msgTime = Calendar.getInstance().getTimeInMillis();
  }

  public long getIndex() {
    return index;
  }
  
  public String getMessage() {
    return message;
  }

  public CharSequence toXML() {
    // Capacity estimated to 40 characters plus message length
    return new StringBuilder(40+message.length()).append("<message index=\"").append(getIndex()).append("\" epoch=\"")
          .append(msgTime).append("\">")
          .append(message).append("</message>");
  }

  @Override
  public String toString() {
    return new StringBuilder(12+message.length()).append(index).append(": ").append(message).toString();
  }
  
  public long getMsgTime() {
    return msgTime;
  }
  
  public GregorianCalendar getMsgDate() {
    // Explicit time so as to not to incur cost of getting current time
    GregorianCalendar calendar = new GregorianCalendar(1970, 1, 1);
    calendar.setTimeInMillis(msgTime);
    return calendar;
  }
  
}
