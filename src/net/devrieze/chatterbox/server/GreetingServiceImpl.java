package net.devrieze.chatterbox.server;


import javax.jdo.PersistenceManagerFactory;

import net.devrieze.chatterbox.client.GreetingService;
import net.devrieze.chatterbox.shared.FieldVerifier;

import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;


/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GreetingServiceImpl extends RemoteServiceServlet implements GreetingService {

  private int seqNo = 0;

  private ChannelManager channelManager = new ChannelManager();

  public String greetServer(String input) throws IllegalArgumentException {
    if (input.startsWith("<connect>")) {
      return channelManager.createChannel();
    }

    // Verify that the input is valid. 
    if (!FieldVerifier.isValidName(input)) {
      // If the input is not valid, throw an IllegalArgumentException back to
      // the client.
      throw new IllegalArgumentException("Name must be at least 4 characters long");
    }

    channelManager.sendMessageToChannels(input);

    return Integer.toString(seqNo++);
  }

  /**
   * Escape an html string. Escaping data received from the client helps to
   * prevent cross-site script vulnerabilities.
   * 
   * @param html the html string to escape
   * @return the escaped string
   */
  private String escapeHtml(String html) {
    if (html == null) {
      return null;
    }
    return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  }
}
