package net.devrieze.chatterbox.server;


import net.devrieze.chatterbox.client.GreetingService;
import net.devrieze.chatterbox.shared.FieldVerifier;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;


/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GreetingServiceImpl extends RemoteServiceServlet implements GreetingService {

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

    Message message = channelManager.sendMessageToChannels(input);

    return "<messages>"+message.toXML()+"</messages>";
  }
}
