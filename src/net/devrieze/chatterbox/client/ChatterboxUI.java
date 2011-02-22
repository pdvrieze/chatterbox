package net.devrieze.chatterbox.client;

import net.devrieze.chatterbox.shared.FieldVerifier;

import com.google.gwt.appengine.channel.client.Channel;
import com.google.gwt.appengine.channel.client.ChannelFactory;
import com.google.gwt.appengine.channel.client.ChannelFactory.ChannelCreatedCallback;
import com.google.gwt.appengine.channel.client.Socket;
import com.google.gwt.appengine.channel.client.SocketError;
import com.google.gwt.appengine.channel.client.SocketListener;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Window;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ChatterboxUI extends Composite implements ChannelCreatedCallback, UpdateMessageEvent.Handler, MoveMessagesEvent.Handler, StatusEvent.Handler {

  private String channelToken;
  
  private EventBus eventBus = new SimpleEventBus();
  
  private final class ConnectCallback implements AsyncCallback<String> {

    public void onFailure(Throwable caught) {
      errorLabel.setText("Connect failed, retrying in 3 seconds");
      new Timer() {
        
        @Override
        public void run() {
          greetingService.greetServer("<connect></connect>", ConnectCallback.this);
          
        }
      }.schedule(3000);
    }

    public void onSuccess(String result) {
      channelToken = result.replace("<token>", "").replace("</token>", "");
      errorLabel.setText("Connect to server succeeded, creating channel... ("+channelToken+")");
      createChannel(channelToken);
    }
  }

  private final class ChannelSocketListener implements SocketListener {

    @Override
    public void onOpen() {
      errorLabel.setText("channel opened");
      // ignore
    }

    @Override
    public void onMessage(String message) {
      messageQueue.handleMessagesReceived(message);
//      addOutput(message);
      errorLabel.setText("Received channel message");
    }

    @Override
    public void onError(SocketError error) {
      errorLabel.setText("Failure to open channel ("+error.getCode()+"):"+error.getDescription());
    }

    @Override
    public void onClose() {
      addOutput("<div style='background-color:red'>channel closed</div>");
      textBox.setEnabled(false);
    }
  }

  interface ChatterboxBinder extends UiBinder<Widget, ChatterboxUI> {}
  private static ChatterboxBinder uiBinder = GWT.create(ChatterboxBinder.class);
  
  interface MyStyle extends CssResource {
    String even();
  }
  @UiField MyStyle style;

  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while attempting to contact the server. Please check your network "
      + "connection and try again.";

  /**
   * Create a remote service proxy to talk to the server-side Greeting service.
   */
  private final GreetingServiceAsync greetingService = GWT.create(GreetingService.class);

  @UiField Button sendButton;
  @UiField Label errorLabel;
  @UiField TextBox textBox;
  @UiField DivElement outputdiv;
  Channel channel = null;

  private ChatterBoxQueue messageQueue;

  private Socket channelSocket;

  public ChatterboxUI() {
    initWidget(uiBinder.createAndBindUi(this));
    
    textBox.setText("GWT User");

    // We can add style names to widgets
    sendButton.addStyleName("sendButton");
    
    eventBus.addHandler(UpdateMessageEvent.TYPE, this);
    eventBus.addHandler(StatusEvent.TYPE, this);
    
    messageQueue = new ChatterBoxQueue(eventBus, true);
    messageQueue.requestMessages();
    
    // Focus the cursor on the name field when the app loads
    
    //greetingService.greetServer("<connect></connect>", new ConnectCallback());
  }

  private void createChannel(String key) {
    ChannelFactory.createChannel(key, this);
  }
  
  @Override
  public void onChannelCreated(Channel ch) {
    channel = ch;
    channelSocket = channel.open(new ChannelSocketListener());
  }

  @UiHandler("textBox")
  void handleKeyUp(KeyUpEvent event) {
    if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
      sendMessage();
    } else {
      sendButton.setEnabled(textBox.getText().length()>=4);
    }
  }

  @UiHandler("sendButton")
  void handleClick(ClickEvent e) {
    sendMessage();
    
  }

  private void sendMessage() {
    String textToServer = textBox.getText();
    if (!FieldVerifier.isValidName(textToServer)) {
      errorLabel.setText("Please enter at least four characters");
      return;
    }

    // Then, we send the input to the server.
    sendButton.setEnabled(false);
    greetingService.greetServer(textToServer, new AsyncCallback<String>() {

      public void onFailure(Throwable caught) {
        errorLabel.setText("Remote Procedure Call - Failure");
        textBox.setText("");
        textBox.setFocus(true);
      }

      public void onSuccess(String result) {
        messageQueue.handleMessagesReceived(result);
//        addOutput(result);
//        errorLabel.setText("RPC successful");
        textBox.setText("");
        textBox.setFocus(true);
      }
    });
  }

  private void addOutput(String text) {
    DivElement d = outputdiv.getOwnerDocument().createDivElement();
    d.setInnerHTML(text);
    boolean even = outputdiv.getChildCount()% 2 == 1;
    if (even) {
      d.addClassName(style.even()); 
    }
    outputdiv.appendChild(d);
  }

  @Override
  public void onMoveMessages(MoveMessagesEvent event) {
    // TODO do this better;
    StringBuilder innerHtml = new StringBuilder();
    int i = 0;
    for(Message m: messageQueue.getMessages()) {
      String mHtml = createMessageHtml(i, m);
      if (mHtml!=null && mHtml.length()>0) {
        i++;
        innerHtml.append(mHtml);
      }

    }
    
    outputdiv.setInnerHTML(innerHtml.toString());
  }

  @Override
  public void onUpdateMessage(UpdateMessageEvent event) {
    // TODO do this much better
    
    // Special case message added
    if (event.getMessageIndex()==messageQueue.size()-1) {
      Message m = messageQueue.getMessages().get(event.getMessageIndex());
      DivElement d = outputdiv.getOwnerDocument().createDivElement();
      d.setInnerHTML(createMessageHtml(outputdiv.getChildCount(), m));
      for(Node n = d.getFirstChild(); n!=null; n=n.getNextSibling()) {
        outputdiv.appendChild(n);
      }
    } else {
      // Pretend we can't be smart and just replace the whole list
      onMoveMessages(null);
    }
  }

  /**
   * Create the right HTML text to put a message in the output div
   * @param i position in the list
   * @param m message
   * @return the html text
   */
  private String createMessageHtml(int i, Message m) {
    StringBuilder mHtml = new StringBuilder(); 
    if (m!=null) {
      if (i%2==1) {
        mHtml.append("<div class=\"even\">");
      } else {
        mHtml.append("<div class=\"odd\">");
      }
      mHtml.append(m.getContent().toString());
      mHtml.append("</div>");
 
    }
    return mHtml.toString();
  }

  @Override
  public void onStatusUpdate(StatusEvent e) {
    String messageText = e.getStatusLevel()+": "+e.getMessage();
    errorLabel.setText(messageText);
    GWT.log(messageText, e.getException());
  }

}
