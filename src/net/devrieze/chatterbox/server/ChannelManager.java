package net.devrieze.chatterbox.server;

import java.util.Iterator;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;

import com.google.appengine.api.channel.ChannelFailureException;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;

public class ChannelManager {

  private ChannelService channelService;

  public ChannelManager() {
    this.channelService = ChannelServiceFactory.getChannelService();
  }

  /**
   * Create a new channel for the client and connect the client to it.
   * @param greetingServiceImpl TODO
   * @return The text needed to retrieve the token by the client.
   */
  String createChannel() {
    String token;
    String clientid;
    PersistenceManager pm = ChatterboxServlet.getPMF().getPersistenceManager();
    try {
      TokenList tokens = getTokenList(pm);
      
      clientid=tokens.getClientId();;
      token = channelService.createChannel(clientid);
      tokens.add(clientid);
//      System.out.println("Channel token: \""+token+"\" key:\""+clientid+"\"");
    } finally {
      pm.close();
    }
    return "<channel>" + token + "</channel>";
  }

  Message sendMessageToChannels(String messageBody) {
    TokenList tokens;
    PersistenceManager pm = ChatterboxServlet.getPMF().getPersistenceManager();
    try {
      Box box = getDefaultBox(pm);
      Message message = box.addMessage(messageBody);
      tokens = getTokenList(pm);
      if (tokens !=null) {
        Iterator<String> it= tokens.iterator();
        while (it.hasNext()) {
          String clientid = it.next();
          try {
//            System.out.println("Sending message to: \""+clientid+"\"");
            channelService.sendMessage(new ChannelMessage(clientid, "<messages>"+message.toXML()+"</messages>"));
          } catch (ChannelFailureException e) {
//            System.out.println("Invalidating channel: "+clientid);
            it.remove();
          }
        }
      }
      return message;
    } finally {
      pm.close();
    }
  }

  private Box getDefaultBox(PersistenceManager pm) {
    try {
      return pm.getObjectById(Box.class, "defaultBox");
    } catch (JDOObjectNotFoundException e) {
      return pm.makePersistent(new Box("defaultBox"));
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
}