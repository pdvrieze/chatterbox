package net.devrieze.chatterbox.client;

import net.devrieze.chatterbox.client.StatusEvent.StatusLevel;
import net.devrieze.chatterbox.shared.FieldVerifier;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ChatterboxUI extends Composite implements UpdateMessageEvent.Handler, MoveMessagesEvent.Handler, StatusEvent.Handler {

  public static final String LOGGER = "chatterbox";

  private EventBus eventBus = new SimpleEventBus();

  interface ChatterboxBinder extends UiBinder<Widget, ChatterboxUI> {}
  private static ChatterboxBinder uiBinder = GWT.create(ChatterboxBinder.class);

  interface MyStyle extends CssResource {
    String even();
  }

  @UiField MyStyle style;

  @UiField Button sendButton;
  @UiField Label errorLabel;
  @UiField TextBox textBox;
  @UiField DivElement outputdiv;
  @UiField AnchorElement logoutRef;

  private ChatterBoxQueue messageQueue;

  public ChatterboxUI() {
    initWidget(uiBinder.createAndBindUi(this));

    textBox.setText("");

    // We can add style names to widgets
    sendButton.addStyleName("sendButton");
    sendButton.setEnabled(false); // Until there is a text

    eventBus.addHandler(UpdateMessageEvent.TYPE, this);
    eventBus.addHandler(StatusEvent.TYPE, this);

    messageQueue = new ChatterBoxQueue(eventBus, true);
    messageQueue.requestMessages();
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
    messageQueue.sendMessage(textToServer);
    textBox.setText("");
    textBox.setFocus(true);
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
    scrollDown();
  }

  private void scrollDown() {
    Document document = Document.get();
    int height = document.getScrollHeight();
    Window.scrollTo(0, height);
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
    if (e.getStatusLevel()==StatusLevel.WARNING) {
      errorLabel.setText(messageText);
    }
    GWT.log(messageText, e.getException());
  }

}
