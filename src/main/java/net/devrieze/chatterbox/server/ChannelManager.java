package net.devrieze.chatterbox.server;

import org.atmosphere.cpr.*;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.naming.NamingException;

import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ChannelManager extends WebSocketEventListenerAdapter {

  private final Broadcaster aBroadCaster;

  public ChannelManager(Broadcaster broadcaster) {
    if (broadcaster==null) { throw new IllegalArgumentException(); }
    aBroadCaster = broadcaster;
  }


  Message createNewMessageAndNotify(String messageBody, Principal pSender) throws SQLException, NamingException {
    Message message;
    try (Connection connection = Util.getConnection(ChatboxManager.CHATBOX_DB_RESOURCE)) {
      Box box = getDefaultBox(connection);
      message = box.addMessage(connection, messageBody, pSender);
      connection.commit();
    }

    aBroadCaster.broadcast(message);

    return message;
  }

  private Box getDefaultBox(@NotNull Connection pConnection) throws SQLException {
    return ChatboxManager.getBox(pConnection, ChatterboxServlet.DEFAULT_BOX);
  }

  public void destroy() {
    for(AtmosphereResource resource: aBroadCaster.getAtmosphereResources()) {
      resource.resume();
    }
    aBroadCaster.destroy();
    Logger.getAnonymousLogger().log(Level.INFO, "Shutting down atmosphere broadcaster");
  }

  public Broadcaster getBroadcaster() {
    return aBroadCaster;
  }
}