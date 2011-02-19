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

  @PrimaryKey
  @Persistent
  private String key = DEFAULTKEY;
  
  @Persistent
  private List<String> tokens;
  
  public List<String> getTokens() {
    if (tokens==null) {
      tokens = new ArrayList<String>(); 
    }
    return tokens;
  }
  
  @Persistent
  private String canary = "canary";

  public void add(String token) {
    getTokens().add(token);
  }

  public Iterator<String> iterator() {
    return getTokens().iterator();
  }

  public int size() {
    return getTokens().size();
  }
  
//  private List<String> getTokens() {
//    if (tokens == null) { tokens = new ArrayList<String>(); }
//    return tokens;
//  }

}
