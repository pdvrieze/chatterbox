package net.devrieze.chatterbox.server;

import java.util.Iterator;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import net.devrieze.chatterbox.client.GreetingService;
import net.devrieze.chatterbox.shared.FieldVerifier;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;


/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GreetingServiceImpl extends RemoteServiceServlet implements GreetingService {

  ChannelService channelService = ChannelServiceFactory.getChannelService();

  int seqNo = 0;

  int clientNo = 0;

  private static PersistenceManagerFactory _pmf = null;

  public String greetServer(String input) throws IllegalArgumentException {
    if (input.startsWith("<connect>")) {
      return createChannel();
    }

    // Verify that the input is valid. 
    if (!FieldVerifier.isValidName(input)) {
      // If the input is not valid, throw an IllegalArgumentException back to
      // the client.
      throw new IllegalArgumentException("Name must be at least 4 characters long");
    }

    String serverInfo = getServletContext().getServerInfo();
    String userAgent = getThreadLocalRequest().getHeader("User-Agent");

    // Escape data from the client to avoid cross-site script vulnerabilities.
    input = escapeHtml(input);
    userAgent = escapeHtml(userAgent);


    String message = "Hello, " + input + "!<br><br>I am running " + serverInfo + ".<br><br>It looks like you are using:<br>" + userAgent;
    sendMessageToChannels(message);

    return Integer.toString(seqNo++);
  }

  /**
   * Create a new channel for the client and connect the client to it.
   * @return The text needed to retrieve the token by the client.
   */
  private String createChannel() {
    String token;
    PersistenceManager pm = getPMF().getPersistenceManager();
    try {
      TokenList tokens = getTokenList(pm);
      token = channelService.createChannel("client" + tokens.size()+1);
      tokens.add(token);
      System.out.println("Channel token: \""+token+"\"");
    } finally {
      pm.close();
    }
    return "<token>" + token + "</token>";
  }

  private void sendMessageToChannels(String message) {
    TokenList tokens;
    PersistenceManager pm = getPMF().getPersistenceManager();
    try {
      tokens = getTokenList(pm);
    } finally {
      pm.close();
    }
    if (tokens !=null) {
      Iterator<String> it= tokens.iterator();
      while (it.hasNext()) {
        String token = it.next();
        try {
          System.out.println("Sending message to: \""+token+"\"");
          channelService.sendMessage(new ChannelMessage(token, message));
        } catch (RuntimeException e) {
          e.printStackTrace();
          //it.remove();
        }
      }
    }
  }

  private TokenList getTokenList(PersistenceManager pm) {
    try {
      return pm.getObjectById(TokenList.class, TokenList.DEFAULTKEY);
    } catch (JDOObjectNotFoundException e) {
      TokenList tokens = new TokenList();
      return pm.makePersistent(tokens);
    }
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

  private static PersistenceManagerFactory getPMF() {
    if (_pmf != null)
      return _pmf;
    _pmf = JDOHelper.getPersistenceManagerFactory("transactions-optional");
    return _pmf;
  }
}
