package net.devrieze.chatterbox.server;

import java.security.Principal;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;

import net.devrieze.annotations.NotNull;
import net.devrieze.util.db.DBConnection;


public class ChannelManager extends WebSocketEventListenerAdapter {

  static final String BROADCASTERNAME = "chatterbox";
  private Broadcaster aBroadCaster;

  Message createNewMessageAndNotify(String messageBody, Principal pSender) throws SQLException {
    Message message;
    try (DBConnection connection = ChatboxManager.getConnection()) {
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

    Broadcaster gwtBroadCaster = BroadcasterFactory.getDefault().lookup(MyGWTCometHandler.BROADCASTERNAME, true);
    if (gwtBroadCaster!=null) {
      gwtBroadCaster.broadcast(message.pojoCopy());
    }

    return message;
  }

  private Box getDefaultBox(@NotNull DBConnection pConnection) throws SQLException {
    return ChatboxManager.getBox(pConnection, ChatterboxServlet.DEFAULT_BOX);
  }

  public Broadcaster getBroadcaster() {
    if (aBroadCaster == null) {
      aBroadCaster = BroadcasterFactory.getDefault().lookup(BROADCASTERNAME, true);
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