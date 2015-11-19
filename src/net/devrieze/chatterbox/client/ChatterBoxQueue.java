package net.devrieze.chatterbox.client;

import java.util.ArrayList;
import java.util.List;

import org.atmosphere.gwt.client.AtmosphereClient;
import org.atmosphere.gwt.client.AtmosphereGWTSerializer;
import org.atmosphere.gwt.client.AtmosphereListener;
import org.atmosphere.gwt.client.SerialTypes;

import net.devrieze.chatterbox.client.StatusEvent.StatusLevel;
import net.devrieze.chatterbox.shared.MessagePojo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import com.google.gwt.xml.client.impl.DOMParseException;


public class ChatterBoxQueue implements Window.ClosingHandler{

  @SerialTypes({MessagePojo.class})
  public abstract static class MessageDeserializer extends AtmosphereGWTSerializer { /* Extension point does not need implementation */ }

  private final class ChannelSocketListener implements AtmosphereListener {

    @Override
    public void onConnected(int pHeartbeat, int pConnectionID) {
      if (!isUseChannel()) {
        if (aAtmosphereClient.isRunning()) {
          aAtmosphereClient.stop();
          eventBus.fireEventFromSource(new StatusEvent(StatusLevel.INFO, "channel opened when close requested, closing it again"),ChatterBoxQueue.this);
        } else {
          eventBus.fireEventFromSource(new StatusEvent(StatusLevel.INFO, "channel opened when close requested, socket lost"),ChatterBoxQueue.this);
        }
      } else {
        eventBus.fireEventFromSource(new StatusEvent(StatusLevel.DEBUG, "channel opened"),ChatterBoxQueue.this);
      }
    }

    @Override
    public void onBeforeDisconnected() {
      eventBus.fireEventFromSource(new StatusEvent(StatusLevel.DEBUG, "channel disconnecting"),ChatterBoxQueue.this);
    }

    @Override
    public void onDisconnected() {
      eventBus.fireEventFromSource(new StatusEvent(StatusLevel.DEBUG, "channel closed"),ChatterBoxQueue.this);
    }

    @Override
    public void onError(Throwable pException, boolean pConnected) {
      setUseChannel(false);
      eventBus.fireEventFromSource(new StatusEvent(StatusLevel.WARNING, "channel error: "+pException.getMessage()),ChatterBoxQueue.this);
    }

    @Override
    public void onHeartbeat() {
      eventBus.fireEventFromSource(new StatusEvent(StatusLevel.DEBUG, "channel heartbeat"),ChatterBoxQueue.this);
    }

    @Override
    public void onRefresh() {
      eventBus.fireEventFromSource(new StatusEvent(StatusLevel.DEBUG, "channel refresh"),ChatterBoxQueue.this);
    }

    @Override
    public void onAfterRefresh() {
      // ignore
    }

    @Override
    public void onMessage(List<?> pMessages) {
      eventBus.fireEventFromSource(new StatusEvent(StatusLevel.DEBUG, "Received channel messages (#"+pMessages.size()+")"),ChatterBoxQueue.this);
      handleMessagesReceived(pMessages);
    }
  }


  public static final int FIRSTMESSAGE = -2;
  public static final int LASTMESSAGE = -1;
  private static final String MESSAGESBASEURL = "chat/messages";
  private static final String CONNECTURL = "gwtcomet";
  private static final String MESSAGEPOSTBASEURL = "chat/messages";

  private ArrayList<Message> messages;
  private long firstMessage = -1;
  private long lastKnownMessage = -1;

  private EventBus eventBus;
  private boolean useChannel;

  private AtmosphereClient aAtmosphereClient=null;

  public ChatterBoxQueue (EventBus eventBus, boolean useChannel) {
    this.eventBus = eventBus;
    this.setUseChannel(useChannel);
    messages = new ArrayList<Message>();
    Window.addWindowClosingHandler(this);
  }

  /**
   * Handler for Asynchronous requests that result in xml messages
   * @param request The request this handler responds to
   * @param response The response to be processed by the handler
   */
  public void handleMessagesReceived(final Request request, Response response) {
    String messageText = response.getText();
    // Just ignore empty messages
    if (messageText.length()>0) {
      handleMessagesReceived(messageText);
    }
  }

  public void handleMessagesReceived(List<?> pMessages) {
    for (Object message: pMessages) {
      if (message instanceof CharSequence) {
        handleMessagesReceived(message.toString());
      } else if (message instanceof List) {
        handleMessagesReceived((List<?>) message);
      } else if (message instanceof MessagePojo) {
        handleMessageReceived((MessagePojo) message);
      }
    }

  }

  public void handleMessageReceived(MessagePojo message) {
    addMessage(new Message(message));
  }


  public void handleMessagesReceived(String messageText) {
    Document message;
    try {
      message = XMLParser.parse(messageText);
    } catch (DOMParseException e) {
      eventBus.fireEventFromSource(new StatusEvent(StatusLevel.WARNING, "Error parsing message \""+messageText+"\"", e), this);
      return;
    }
    Element root = getRootElement(message);
    if ("messages".equals(root.getTagName())) {
      Node n = root.getFirstChild();
      while (n!=null) {
        if (n.getNodeType()==Node.ELEMENT_NODE) {
          handleReceivedMessage((Element) n);
        }
        n = n.getNextSibling();
      }
    } else if ("message".equals(root.getTagName())) {
      handleReceivedMessage(root);
    } else {
      eventBus.fireEventFromSource(new StatusEvent(StatusLevel.WARNING, "Unexpected root node for messages tag: "+message), this);
    }
  }

  private void handleReceivedMessage(Element e) {
    if ("message".equals(e.getTagName())) {
      String index = e.getAttribute("index");
      String epoch = e.getAttribute("epoch");
      String sender = e.getAttribute("from");
      NodeList content = e.getChildNodes();
      if (index!=null) {
        Message m = new Message(index, sender, epoch, content);
        addMessage(m);
      }
    } else {
      eventBus.fireEventFromSource(new StatusEvent(StatusLevel.WARNING, "Unexpected child in messages tag: "+e), this);
    }
  }

  private void addMessage(Message m) {
    if (firstMessage<0) {
      updateFirstMessage();
      firstMessage = m.getIndex();
      if (firstMessage <10) { // optimize speed over small memory use
        firstMessage = 0;
      }
    }
    int listPos = (int) (m.getIndex()-firstMessage);
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
        messages.ensureCapacity((int) (listPos - firstMessage));
        for(int i = messages.size(); i<listPos; ++i) {
          messages.add(null);
        }
        requestMessages(messages.size()+firstMessage, m.getIndex()-1);
      }
      messages.add(m);
      fireUpdateMessage(listPos);
    }
    lastKnownMessage = Math.max(lastKnownMessage, firstMessage+messages.size());
  }

  private void fireUpdateMessage(int listPos) {
    eventBus.fireEventFromSource(new UpdateMessageEvent(listPos), this);
  }

  private void fireUpdateMove(int amount) {
    eventBus.fireEventFromSource(new MoveMessagesEvent(amount), this);

  }

  private void moveMessages(int pListPos) {
    if (pListPos>0) {
      messages.ensureCapacity(messages.size()+pListPos);
      int oldSize = messages.size();
      for(int j=(oldSize-pListPos); j<oldSize; ++j) {
        messages.add(messages.get(j));
      }
      for(int j=oldSize-pListPos-1; j>=0; j--) {
        messages.set(j+pListPos, messages.get(j));
      }
      for(int j=0; j<pListPos; ++j) {
        messages.set(j, null);
      }
      // The head should initiate this so does not need to be requested again
      // Function is inclusive, so current firstMessage can stay
      requestMessages(firstMessage-pListPos+1, firstMessage-1);
    } else if (pListPos<0) {
      int end = messages.size() + pListPos;
      for(int j = 0; j < end; ++j) {
        messages.set(j, messages.get(j-pListPos));
      }
      while(messages.size()>end) {
        messages.remove(messages.size()-1);
      }
    }
    firstMessage-=pListPos;
    fireUpdateMove(pListPos);
  }

  public void requestMessages() {
    requestMessages(FIRSTMESSAGE, LASTMESSAGE);
  }

  /**
   * Initiate requesting a message range. {@link #FIRSTMESSAGE} is a special value for the first message,
   * {@link #LASTMESSAGE} is a special value for the last message.
   * @param start The first message to request.
   * @param end The last message to request.
   */
  public void requestMessages(long start, long end) {
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
      rb.sendRequest(null, new RequestCallback() {

        @Override
        public void onResponseReceived(Request request, Response response) {
          final int statusCode = response.getStatusCode();
          if (statusCode>=200 && statusCode<400) {
            handleMessagesReceived(request, response);
          } else if (statusCode==Response.SC_UNAUTHORIZED) {
            eventBus.fireEventFromSource(new ReLoginEvent(), response);
          } else {
            eventBus.fireEventFromSource(new StatusEvent(StatusLevel.WARNING, "Unexpected response from server: "+statusCode+" "+response.getStatusText()), response);
          }
        }

        @Override
        public void onError(Request request, Throwable exception) {
          eventBus.fireEventFromSource(new StatusEvent(StatusLevel.WARNING, "Requesting messages failed", exception), this);
        }
      });
    } catch (RequestException e) {
      eventBus.fireEventFromSource(new StatusEvent(StatusLevel.WARNING, "Requesting messages failed", e), this);
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

  public List<Message> getMessages() {
    return messages;
  }

  public int size() {
    return messages.size();
  }

  public void setUseChannel(boolean useChannel) {
    if(useChannel ==this.useChannel) {
      return;
    }
    this.useChannel = useChannel;
    if (useChannel) {
      connectToChannel();
    } else {
      disconnectFromChannel();
    }

  }

  private void disconnectFromChannel() {
    if (aAtmosphereClient!=null) {
      aAtmosphereClient.stop();
//      aAtmosphereClient=null;
    }

  }

  private void connectToChannel() {
    ChannelSocketListener listener = new ChannelSocketListener();
    if (aAtmosphereClient==null) {

      AtmosphereGWTSerializer myserializer = GWT.create(MessageDeserializer.class);
      aAtmosphereClient = new AtmosphereClient(GWT.getHostPageBaseURL()+CONNECTURL, myserializer, listener, true);
    }
    aAtmosphereClient.start();
  }

  public boolean isUseChannel() {
    return useChannel;
  }


  @Override
  public void onWindowClosing(ClosingEvent event) {
    setUseChannel(false);
  }

  public void sendMessage(String textToServer) {
    RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, MESSAGEPOSTBASEURL);
    try {
      rb.sendRequest(textToServer, new RequestCallback() {

        @Override
        public void onResponseReceived(Request request, Response response) {
          handleMessagesReceived(request, response);
        }

        @Override
        public void onError(Request request, Throwable exception) {
          eventBus.fireEventFromSource(new StatusEvent(StatusLevel.WARNING, "Sending message failed", exception), this);
        }
      });
    } catch (RequestException e) {
      eventBus.fireEventFromSource(new StatusEvent(StatusLevel.WARNING, "Sending message failed", e), this);
    }
  }

}
