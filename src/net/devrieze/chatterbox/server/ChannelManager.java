package net.devrieze.chatterbox.server;

import java.io.IOException;
import java.security.Principal;

import org.atmosphere.cpr.*;

public class ChannelManager implements AtmosphereHandler {

  private Broadcaster aBroadCaster;

  Message createNewMessageAndNotify(String messageBody, Principal pSender) {
    Box box = getDefaultBox();
    Message message = box.addMessage(messageBody, pSender);
    
    if (aBroadCaster!=null) {
      aBroadCaster.broadcast(message);
    }
    
    return message;
  }

  private Box getDefaultBox() {
    return ChatboxManager.getBox(ChatterboxServlet.DEFAULT_BOX);
  }

  @Override
  public void onRequest(AtmosphereResource pResource) throws IOException {
    if (aBroadCaster == null) {
      aBroadCaster = pResource.getBroadcaster();
    }

    AtmosphereRequest req = pResource.getRequest();

    // First, tell Atmosphere to allow bi-directional communication by suspending.
    if (req.getMethod().equalsIgnoreCase("GET")) {
      // The negotiation header is just needed by the sample to list all the supported transport.
      if (req.getHeader("negotiating") == null) {
        pResource.suspend();
      } else {
        pResource.getResponse().getWriter().write("OK");
      }
      // Second, broadcast message to all connected users.
    } else if (req.getMethod().equalsIgnoreCase("POST")) {
      pResource.getBroadcaster().broadcast(req.getReader().readLine().trim());
    }
  }

  @Override
  public void onStateChange(AtmosphereResourceEvent pEvent) throws IOException {
    AtmosphereResource r = pEvent.getResource();
    AtmosphereResponse res = r.getResponse();

    if (pEvent.isSuspended()) {

      Message message= (Message) pEvent.getMessage();

      res.getWriter().write("<messages>"+message.toXML()+"</messages>");
      switch (r.transport()) {
        case JSONP:
        case AJAX:
        case LONG_POLLING:
          pEvent.getResource().resume();
          break;
        case WEBSOCKET:
        case STREAMING:
          res.getWriter().flush();
          break;
      }
    } else if (!pEvent.isResuming()) {
      pEvent.broadcaster().broadcast("<messages><!-- Bye --></messages>");
    }
  }

  @Override
  public void destroy() {
  }
}