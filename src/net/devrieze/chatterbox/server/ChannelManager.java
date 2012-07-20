package net.devrieze.chatterbox.server;

import java.security.Principal;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;

import net.devrieze.chatterbox.shared.MessagePojo;


public class ChannelManager extends WebSocketEventListenerAdapter {

  static final String BROADCASTERNAME = "chatterbox";
  private Broadcaster aBroadCaster;

  Message createNewMessageAndNotify(String messageBody, Principal pSender) {
    Box box = getDefaultBox();
    Message message = box.addMessage(messageBody, pSender);
    
    if (aBroadCaster==null) {
      getBroadcaster();
    }
    
    if (aBroadCaster!=null) {
      aBroadCaster.broadcast(message.toXML());
    }
    
    Broadcaster gwtBroadCaster = BroadcasterFactory.getDefault().lookup(MyGWTCometHandler.BROADCASTERNAME, true);
    if (gwtBroadCaster!=null) {
      gwtBroadCaster.broadcast(message.toPojo());
    }
    
    return message;
  }

  private Box getDefaultBox() {
    return ChatboxManager.getBox(ChatterboxServlet.DEFAULT_BOX);
  }

  public Broadcaster getBroadcaster() {
    if (aBroadCaster == null) {
      aBroadCaster = BroadcasterFactory.getDefault().lookup(BROADCASTERNAME, true);
    }
    return aBroadCaster;
  }
}