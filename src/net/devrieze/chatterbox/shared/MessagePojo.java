package net.devrieze.chatterbox.shared;

import java.io.Serializable;
import java.security.Principal;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class MessagePojo implements Serializable{

  private static final long serialVersionUID = 5641483443503015088L;

  private Long aIndex;
  
  
  private String aMessageBody;


  private Long aMsgTime;


  private String aSender;

  public MessagePojo() {}

  public MessagePojo(long pIndex, String pMessageBody, long pEpoch, String pSenderEmail) {
    aIndex = pIndex;
    aMessageBody = pMessageBody;
    aMsgTime = pEpoch;
    aSender = pSenderEmail;
    
  }


  public Long getIndex() {
    return aIndex;
  }

  
  public String getMessageBody() {
    return aMessageBody;
  }

  public long getMsgTime() {
    return aMsgTime;
  }

  public String getSender() {
    return aSender;
  }
  
}
