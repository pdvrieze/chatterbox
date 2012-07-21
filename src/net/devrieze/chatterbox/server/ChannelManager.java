package net.devrieze.chatterbox.server;

import java.security.Principal;

import javax.servlet.ServletRequest;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;


public class ChannelManager extends WebSocketEventListenerAdapter {

  static final String BROADCASTERNAME = "chatterbox";
  private Broadcaster aBroadCaster;

  Message createNewMessageAndNotify(String messageBody, Principal pSender, ServletRequest pKey) {
    Box box = getDefaultBox(pKey);
    Message message = box.addMessage(messageBody, pSender);
    
    if (aBroadCaster==null) {
      getBroadcaster();
    }
    
    if (aBroadCaster!=null) {
      aBroadCaster.broadcast(message.toXML());
    }
    
    Broadcaster gwtBroadCaster = BroadcasterFactory.getDefault().lookup(MyGWTCometHandler.BROADCASTERNAME, true);
    if (gwtBroadCaster!=null) {
      gwtBroadCaster.broadcast(message.pojoCopy());
    }
    
    return message;
  }

  private Box getDefaultBox(ServletRequest pKey) {
    return ChatboxManager.getBox(ChatterboxServlet.DEFAULT_BOX, pKey);
  }

  public Broadcaster getBroadcaster() {
    if (aBroadCaster == null) {
      aBroadCaster = BroadcasterFactory.getDefault().lookup(BROADCASTERNAME, true);
    }
    return aBroadCaster;
  }
}