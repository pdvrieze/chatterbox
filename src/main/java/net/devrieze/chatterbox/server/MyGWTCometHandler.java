package net.devrieze.chatterbox.server;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.Serializer;
import org.atmosphere.gwt20.shared.Constants;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;

import javax.servlet.ServletException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.atmosphere.cpr.FrameworkConfig.ATMOSPHERE_CONFIG;


public class MyGWTCometHandler extends AbstractReflectorAtmosphereHandler {
  static final Logger logger = Logger.getLogger("AtmosphereHandler");

  private static final int TIMEOUT = 60000;// Resume after 60 seconds
  static final String BROADCASTERNAME = "CHATBOXBROADCASTER";

  @Override
  public void init(final AtmosphereConfig config) throws ServletException {
    super.init(config);
    config.getServletContext().setAttribute(ATMOSPHERE_CONFIG, config);
  }

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
    final Broadcaster broadcaster = res.getAtmosphereConfig().getBroadcasterFactory().lookup(BROADCASTERNAME, true);
    res.getResponse().setContentType("text/xml");
    res.setBroadcaster(broadcaster);
    res.setSerializer(new Serializer() {
      Charset mCharset = Charset.forName(res.getResponse().getCharacterEncoding());
      @Override
      public void write(final OutputStream os, final Object o) throws IOException {
        logger.warning("Writing object to outputstream with charset: "+mCharset.displayName()+"\n  "+o);
        if (o instanceof Message) {
          Message msg = (Message) o;
          os.write(msg.toXML().toString().getBytes(mCharset));
          os.flush();
        }
      }
    });

//    res.suspend(TIMEOUT, TimeUnit.MILLISECONDS);
    res.suspend();
  }

}
