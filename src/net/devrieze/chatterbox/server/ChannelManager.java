package net.devrieze.chatterbox.server;

import java.security.Principal;
import java.util.Arrays;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.gwt.server.AtmosphereGwtHandler;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.mortbay.util.ajax.JSON;

import net.devrieze.util.JSONObject;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONString;

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
      aBroadCaster.broadcast(message.toXML().toString());
    }
    
    Broadcaster gwtBroadCaster = BroadcasterFactory.getDefault().lookup(MyGWTCometHandler.BROADCASTERNAME, true);
    if (gwtBroadCaster!=null) {
      gwtBroadCaster.broadcast(message.toXML().toString());
    }
    
    return message;
  }

  private Box getDefaultBox() {
    return ChatboxManager.getBox(ChatterboxServlet.DEFAULT_BOX);
  }
/*
  @Override
  public void onRequest(AtmosphereResource pResource) throws IOException {
    if (aBroadCaster == null) {
      aBroadCaster = BroadcasterFactory.getDefault().lookup(BROADCASTERNAME, true);
      
    }

    AtmosphereRequest req = pResource.getRequest();

    // First, tell Atmosphere to allow bi-directional communication by suspending.
    if (req.getMethod().equalsIgnoreCase("GET")) {
      pResource.setBroadcaster(aBroadCaster);
      // The negotiation header is just needed by the sample to list all the supported transport.
      if (req.getHeader("negotiating") == null) {
        pResource.getResponse().getWriter().write("<!-- connected "+aBroadCaster.getID()+" -->");
        pResource.suspend();
      } else {
        pResource.getResponse().getWriter().write("OK");
      }
      // Second, broadcast message to all connected users.
    } else if (req.getMethod().equalsIgnoreCase("POST")) {
      // ignore
      
//      Principal sender = req.getUserPrincipal();
//      int contentLength = req.getContentLength();
//      BufferedReader in = req.getReader();
//
//      // Assume most text will be ascii and as such contentlength == string length
//      char[] buffer = new char[contentLength];
//      StringBuilder message = new StringBuilder(contentLength);
//      {
//        int read =  in.read(buffer);
//        while (read >=0) {
//          message.append(buffer,0,read);
//          read = in.read(buffer);
//        }
//      }
//      if (message.length()>0) {
//        createNewMessageAndNotify(message.toString(), sender);
//      }
    }
  }

  @Override
  public void onStateChange(AtmosphereResourceEvent pEvent) throws IOException {
    AtmosphereResource r = pEvent.getResource();
    AtmosphereResponse res = r.getResponse();

    if (pEvent.isSuspended()) {

      Message message= (Message) pEvent.getMessage();

      res.getWriter().write("<messages>"+message.toXML()+"</messages>\n");
      switch (r.transport()) {
        case JSONP:
        case AJAX:
        case POLLING:
        case LONG_POLLING:
          pEvent.getResource().resume();
          break;
        case WEBSOCKET:
        case STREAMING:
          res.getWriter().flush();
          break;
        default:
          res.getWriter().flush();
            
      }
    } else if (!pEvent.isResuming()) {
      pEvent.broadcaster().broadcast("<messages><!-- Bye --></messages>\n");
    }
  }

  @Override
  public void destroy() {
    if (aBroadCaster!=null) {
      for (AtmosphereResource resource : aBroadCaster.getAtmosphereResources()) {
        resource.resume();
      }
    }
  }
*/
  public Broadcaster getBroadcaster() {
    if (aBroadCaster == null) {
      aBroadCaster = BroadcasterFactory.getDefault().lookup(BROADCASTERNAME, true);
    }
    return aBroadCaster;
  }
}