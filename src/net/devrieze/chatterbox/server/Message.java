package net.devrieze.chatterbox.server;

import java.security.Principal;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Message {
  
  private Long aIndex;
  
  private String aMessageBody;
  
  private Long aMsgTime;

  private String aSender;

  public Message(long pIndex, String pMessage, Principal pSender) {
    this.aIndex = pIndex;
    this.aMessageBody = pMessage;
    this.aSender = UserManager.getCurrentUserEmail(pSender); 
    aMsgTime = Calendar.getInstance().getTimeInMillis();
  }
  
  public Message(long pIndex, String pMessageBody, long pEpoch, String pSenderEmail) {
    aIndex = pIndex;
    aMessageBody = pMessageBody;
    aMsgTime = pEpoch;
    aSender = pSenderEmail;
    
  }

  public long getIndex() {
    return aIndex;
  }
  
  public String getMessage() {
    return aMessageBody;
  }

  public CharSequence toXML() {
    // Capacity estimated to 40 characters plus message length
    StringBuilder result = new StringBuilder(40+aMessageBody.length()).append("<message index=\"").append(getIndex());
    result.append("\" epoch=\"").append(aMsgTime);
    if (getSender()!=null) { result.append("\" from=\"").append(Util.encodeHtml(getSender())); }
    result.append("\">").append(aMessageBody).append("</message>");
    return result;
  }

  @Override
  public String toString() {
    return new StringBuilder(12+aMessageBody.length()).append(aIndex).append(": ").append(aMessageBody).toString();
  }
  
  public long getMsgTime() {
    return aMsgTime;
  }
  
  public GregorianCalendar getMsgDate() {
    // Explicit time so as to not to incur cost of getting current time
    GregorianCalendar calendar = new GregorianCalendar(1970, 1, 1);
    calendar.setTimeInMillis(aMsgTime);
    return calendar;
  }

  public String getSender() {
    return aSender;
  }
  
}
