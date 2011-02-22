package net.devrieze.chatterbox.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class TokenList {

//  public static final Key DEFAULTKEY = KeyFactory.createKey(TokenList.class.getSimpleName(), "tokenList");
  @NotPersistent
  public static final String DEFAULTKEY = "defaultChatChannel";

  @SuppressWarnings("unused")
  @PrimaryKey
  @Persistent
  private String key = DEFAULTKEY;
  
  @Persistent
  private List<String> tokens;
  
  @Persistent
  private Integer nextClientId;
  
  public List<String> getTokens() {
    if (tokens==null) {
      tokens = new ArrayList<String>(); 
    }
    return tokens;
  }

  public void add(String token) {
    getTokens().add(token);
  }

  public Iterator<String> iterator() {
    return getTokens().iterator();
  }

  public int size() {
    return getTokens().size();
  }

  private synchronized void setNextClientId(int nextClientId) {
    this.nextClientId = nextClientId;
  }

  public synchronized int getNextClientId() {
    if (nextClientId==null) { nextClientId = 1; }
    return nextClientId;
  }

  public String getClientId() {
    int nextId;
    synchronized (this) {
      nextId = getNextClientId();
      setNextClientId(nextId+1);
    }
    return "client"+nextId;
  }
  
  
  
//  private List<String> getTokens() {
//    if (tokens == null) { tokens = new ArrayList<String>(); }
//    return tokens;
//  }

}
