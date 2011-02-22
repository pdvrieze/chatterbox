package net.devrieze.chatterbox.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;


public class ChatterBoxQueue {
  
  public static final int FIRSTMESSAGE = -2;
  public static final int LASTMESSAGE = -1;
  private static final String MESSAGESBASEURL = "chat/messages";
  
  private ArrayList<Message> messages;
  private int firstMessage = -1;
  private int lastKnownMessage = -1;
  private boolean working = true;
  private EventBus eventBus;
  
  public ChatterBoxQueue (EventBus eventBus) {
    this.eventBus = eventBus;
    messages = new ArrayList<Message>();
  }
  
  public void handleMessagesReceived(Request request, Response response) {
    Document message = XMLParser.parse(response.getText());
    Element root = getRootElement(message);
    if ("messages".equals(root.getTagName())) {
      Node n = root.getFirstChild();
      while (n!=null) {
        if (n.getNodeType()==Node.ELEMENT_NODE) {
          Element e = (Element) n;
          if ("message".equals(e.getTagName())) {
            String index = e.getAttribute("index");
            String epoch = e.getAttribute("epoch");
            NodeList content = e.getChildNodes();
            if (index!=null) {
              Message m = new Message(index, epoch, content);
              addMessage(m);
            }
          } else {
            GWT.log("Unexpected child in messages tag: "+e);
          }
          
        }
        n = n.getNextSibling();
      } 
    } else {
      GWT.log("Unexpected root node for messages tag: "+message);
    }
    
  }

  private void addMessage(Message m) {
    boolean updateAll = false;
    if (firstMessage<0) {
      updateFirstMessage();
      firstMessage = m.getIndex(); 
      if (firstMessage <10) { // optimize speed over small memory use
        firstMessage = 0;
      }
    }
    int listPos = m.getIndex()-firstMessage;
    if (listPos<0) {
      moveMessages(-listPos);
      listPos = 0;
    }
    if (listPos <messages.size()) {
      // For now assume that messages don't change
      if (messages.get(listPos)==null) {
        messages.set(listPos, m);
        fireUpdateMessage(listPos);
      }
    } else { // Append a message
      if (listPos > messages.size()) {
        messages.ensureCapacity(listPos - firstMessage);
        for(int i = messages.size(); i<listPos; ++i) {
          messages.add(null);
        }
        requestMessages(messages.size()+firstMessage, m.getIndex()-1);
      }
      messages.add(m);
      fireUpdateMessage(listPos);
    }
    lastKnownMessage = Math.min(lastKnownMessage, firstMessage+messages.size());
  }

  private void fireUpdateMessage(int listPos) {
    eventBus.fireEventFromSource(new UpdateMessageEvent(listPos), this);
  }

  private void fireUpdateMove(int amount) {
    eventBus.fireEventFromSource(new MoveMessagesEvent(amount), this);
    
  }

  private void moveMessages(int i) {
    if (i>0) {
      messages.ensureCapacity(messages.size()+i);
      int oldSize = messages.size();
      for(int j=(oldSize-i); j<oldSize; ++j) {
        messages.add(messages.get(j));
      }
      for(int j=oldSize-i-1; j>=0; j--) {
        messages.set(j+i, messages.get(j));
      }
      for(int j=0; j<i; ++j) {
        messages.set(j, null);
      }
      // The head should initiate this so does not need to be requested again
      // Function is inclusive, so current firstMessage can stay
      requestMessages(firstMessage-i+1, firstMessage-1);
    } else if (i<0) {
      int end = messages.size() + i;
      for(int j = 0; j < end; ++j) {
        messages.set(j, messages.get(j-i));
      }
      while(messages.size()>end) {
        messages.remove(messages.size()-1);
      }
    }
    firstMessage-=i;
    fireUpdateMove(i);
  }

  /**
   * Initiate requesting a message range. {@link #FIRSTMESSAGE} is a special value for the first message,
   * {@link #LASTMESSAGE} is a special value for the last message.
   * @param start The first message to request.
   * @param end The last message to request.
   */
  private void requestMessages(int start, int end) {
    StringBuilder requestURL= new StringBuilder();
    requestURL.append(MESSAGESBASEURL);
    char parChar = '?';
    if(start==LASTMESSAGE) {
      requestURL.append(parChar).append("start=last");
      parChar = '&';
    } else if (start>=0) {
      requestURL.append(parChar).append("start=").append(start);
      parChar= '&';
    }
    if(end==FIRSTMESSAGE) {
      requestURL.append(parChar).append("end=last");
      parChar = '&';
    } else if (end>=0) {
      requestURL.append(parChar).append("end=").append(end);
      parChar= '&';
    }
    
    RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, requestURL.toString());
    try {
      Request request = rb.sendRequest(null, new RequestCallback() {
        
        @Override
        public void onResponseReceived(Request request, Response response) {
          handleMessagesReceived(request, response);
        }
        
        @Override
        public void onError(Request request, Throwable exception) {
          working = false;
          GWT.log("Requesting messages failed", exception);
        }
      });
    } catch (RequestException e) {
      working = false;
      GWT.log("Requesting messages failed", e);
    }
    
  }

  /**
   * Ask server what is the first message
   */
  private void updateFirstMessage() {
    requestMessages(FIRSTMESSAGE, FIRSTMESSAGE);
  }

  private Element getRootElement(Document d) {
    Node n = d.getFirstChild();
    while (n!=null && n.getNodeType()!=Node.ELEMENT_NODE) {
      n = n.getNextSibling();
    }
    return (Element) n;
  }

}
