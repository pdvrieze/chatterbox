package net.devrieze.chatterbox.server;

import java.io.IOException;

import javax.servlet.ServletException;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.gwt.server.AtmosphereGwtHandler;
import org.atmosphere.gwt.server.GwtAtmosphereResource;


public class MyGWTCometHandler extends AtmosphereGwtHandler {

  @Override
  public int doComet(GwtAtmosphereResource pResource) throws ServletException, IOException {
    Broadcaster broadcaster = BroadcasterFactory.getDefault().lookup(BROADCASTERNAME, true);
    pResource.getAtmosphereResource().setBroadcaster(broadcaster);
    return TIMEOUT;
  }

  private static final int TIMEOUT = 60000;// Resume after 60 seconds
  static final String BROADCASTERNAME = GWT_BROADCASTER_ID;

  @Override
  public void onRequest(AtmosphereResource pResource) throws IOException {
    try {
      super.onRequest(pResource);
    } catch (IllegalStateException e) {
      // Transport could not be determined. Just handle like regular request
      if ("GET".equals(pResource.getRequest().getMethod())) {
        pResource.suspend(TIMEOUT, false);
      }
    }
  }

}
