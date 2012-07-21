package net.devrieze.chatterbox.server;

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.devrieze.chatterbox.shared.MessagePojo;


public class Message extends MessagePojo{

  private static final long serialVersionUID = 1298603868339441494L;
  
  public Message(MessagePojo pPojo) {
    super(pPojo);
  }

  public Message(long pIndex, String pMessage, String pSenderEmail) {
    this(pIndex, pMessage, Calendar.getInstance().getTimeInMillis(), pSenderEmail);
  }
  
  public Message(long pIndex, String pMessageBody, long pEpoch, String pSenderEmail) {
    super(pIndex, pMessageBody, pEpoch, pSenderEmail);
    
  }
  
  @Deprecated
  public String getMessage() {
    return getMessageBody();
  }

  public CharSequence toXML() {
    final String messageBody = getMessageBody();
    // Capacity estimated to 40 characters plus message length
    final StringBuilder result = new StringBuilder(40+messageBody.length()).append("<message index=\"").append(getIndex());
    result.append("\" epoch=\"").append(getMsgTime());
    if (getSender()!=null) { result.append("\" from=\"").append(Util.encodeHtml(getSender())); }
    result.append("\">").append(messageBody).append("</message>\n");
    return result;
  }

  @Override
  public String toString() {
    final String messageBody = getMessageBody();
    return new StringBuilder(12+messageBody.length()).append(getIndex()).append(": ").append(messageBody).toString();
  }
  
  public GregorianCalendar getMsgDate() {
    // Explicit time so as to not to incur cost of getting current time
    GregorianCalendar calendar = new GregorianCalendar(1970, 1, 1);
    calendar.setTimeInMillis(getMsgTime());
    return calendar;
  }
  
}
