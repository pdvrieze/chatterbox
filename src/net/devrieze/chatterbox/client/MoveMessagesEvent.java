package net.devrieze.chatterbox.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;


public class MoveMessagesEvent extends GwtEvent<MoveMessagesEvent.Handler> {
  
  public static interface Handler extends EventHandler {
    void onMoveMessages(MoveMessagesEvent event);
  }
  
  public static Type<Handler> type = new Type<MoveMessagesEvent.Handler>();
  private final int moveAmount;

  public MoveMessagesEvent(int moveAmount) {
    this.moveAmount = moveAmount;
    
  }
  
  @Override
  public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
    return type;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onMoveMessages(this);
  }

  public int getMoveAmount() {
    return moveAmount;
  }

}
