package net.devrieze.chatterbox.server;

import java.io.IOException;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.gwt.server.AtmosphereGwtHandler;


public class MyGWTCometHandler extends AtmosphereGwtHandler {

  private static final long MAX_TIMEOUT = 300000l;
  static final String BROADCASTERNAME = GWT_BROADCASTER_ID;

  @Override
  public void onRequest(AtmosphereResource pResource) throws IOException {
    Broadcaster broadcaster = BroadcasterFactory.getDefault().lookup(BROADCASTERNAME, true);
    pResource.setBroadcaster(broadcaster);
    try {
      super.onRequest(pResource);
    } catch (IllegalStateException e) {
      // Transport could not be determined. Just handle like regular request
      if ("GET".equals(pResource.getRequest().getMethod())) {
        pResource.suspend(MAX_TIMEOUT, false);
      }
    }
  }

}
