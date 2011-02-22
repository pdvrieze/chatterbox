package net.devrieze.chatterbox.client;

import net.devrieze.chatterbox.client.StatusEvent.StatusLevel;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;


public class StatusEvent extends GwtEvent<StatusEvent.Handler> {
  
  
  public enum StatusLevel {
    WARNING,
    INFO,
    DEBUG;
  }

  public interface Handler extends EventHandler{
    public void onStatusUpdate(StatusEvent e);
  }

  public static final Type<Handler> TYPE = new Type<StatusEvent.Handler>();
  private final StatusLevel statusLevel;
  private final String message;
  private final Throwable exception;

  public StatusEvent(StatusLevel statusLevel, String message) {
    this(statusLevel, message, null);
  }

  public StatusEvent(StatusLevel statusLevel, String message, Throwable exception) {
    this.statusLevel = statusLevel;
    this.message = message;
    this.exception = exception;
  }

  @Override
  public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onStatusUpdate(this);
  }

  public StatusLevel getStatusLevel() {
    return statusLevel;
  }

  public String getMessage() {
    return message;
  }

  public Throwable getException() {
    return exception;
  }

}
