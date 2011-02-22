package net.devrieze.chatterbox.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;


public class UpdateMessageEvent extends GwtEvent<UpdateMessageEvent.Handler> {
  
  public static interface Handler extends EventHandler {
    void onUpdateMessage(UpdateMessageEvent event);
  }
  
  public static Type<Handler> type = new Type<UpdateMessageEvent.Handler>();
  private final int messageIndex;

  public UpdateMessageEvent(int messageIndex) {
    this.messageIndex = messageIndex;
    
  }
  
  @Override
  public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
    return type;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onUpdateMessage(this);
  }

  public int getMessageIndex() {
    return messageIndex;
  }

}
