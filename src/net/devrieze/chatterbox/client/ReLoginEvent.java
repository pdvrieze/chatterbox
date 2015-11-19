package net.devrieze.chatterbox.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;


public class ReLoginEvent extends GwtEvent<ReLoginEvent.Handler> {


  public interface Handler  extends EventHandler{
    public void onReLogin(ReLoginEvent e);
  }

  public static final Type<Handler> TYPE = new Type<ReLoginEvent.Handler>();

  @Override
  public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(Handler pHandler) {
    pHandler.onReLogin(this);
  }

}
