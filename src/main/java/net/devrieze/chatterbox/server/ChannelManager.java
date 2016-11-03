package net.devrieze.chatterbox.server;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcasterFactory;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.naming.NamingException;

import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ChannelManager extends WebSocketEventListenerAdapter {

  static final String BROADCASTERNAME = "chatterbox";
  private Broadcaster aBroadCaster;

  Message createNewMessageAndNotify(String messageBody, Principal pSender) throws SQLException, NamingException {
    Message message;
    try (Connection connection = Util.getConnection(ChatboxManager.CHATBOX_DB_RESOURCE)) {
      Box box = getDefaultBox(connection);
      message = box.addMessage(connection, messageBody, pSender);
      connection.commit();
    }

    if (aBroadCaster==null) {
      getBroadcaster();
    }

    if (aBroadCaster!=null) {
      aBroadCaster.broadcast(message.toXML());
    }

    return message;
  }

  private Box getDefaultBox(@NotNull Connection pConnection) throws SQLException {
    return ChatboxManager.getBox(pConnection, ChatterboxServlet.DEFAULT_BOX);
  }

  public Broadcaster getBroadcaster() {
    if (aBroadCaster == null) {
      aBroadCaster = (new DefaultBroadcasterFactory()).lookup(MyGWTCometHandler.BROADCASTERNAME, true);
    }
    return aBroadCaster;
  }

  public void destroy() {
    if (aBroadCaster!=null) {
      for(AtmosphereResource resource: aBroadCaster.getAtmosphereResources()) {
        resource.resume();
      }
      aBroadCaster.destroy();
      aBroadCaster=null;
      Logger.getAnonymousLogger().log(Level.INFO, "Shutting down atmosphere broadcaster");
    }
  }
}