package net.devrieze.chatterbox.server;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.gwt20.shared.Constants;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;


public class MyGWTCometHandler extends AbstractReflectorAtmosphereHandler {
  static final Logger logger = Logger.getLogger("AtmosphereHandler");

  private static final int TIMEOUT = 60000;// Resume after 60 seconds
  static final String BROADCASTERNAME = "CHATBOXBROADCASTER";

  @Override
  public void onRequest(AtmosphereResource res) throws IOException {
    final String method = res.getRequest().getMethod();
    if (method.equals("GET")) {
      doGet(res);
    } else if (method.equals("POST")) {
      doPost(res);
    }
  }

  private void doPost(final AtmosphereResource res) {
    Object msg = res.getRequest().getAttribute(Constants.MESSAGE_OBJECT);
    if (msg!=null) {
      logger.info("Received RPC post (ignored): "+msg.toString());
    }
  }

  private void doGet(final AtmosphereResource res) {
    res.setBroadcaster(res.getAtmosphereConfig().getBroadcasterFactory().lookup(BROADCASTERNAME, true));
    res.suspend(TIMEOUT, TimeUnit.MILLISECONDS);
  }

}
