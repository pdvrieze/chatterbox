package net.devrieze.chatterbox.server;

import java.util.Set;
import java.util.TreeSet;

import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class UserList {
  
  @NotPersistent
  public static final String DEFAULTKEY = "userList";

  @SuppressWarnings("unused")
  @PrimaryKey
  @Persistent
  private String key = DEFAULTKEY;
  
  @Persistent
  private Set<String> users;

  public Set<String> getPrincipals() {
    if (users==null) {
      users = new TreeSet<String>(); 
    }
    return users;
  }

  public boolean contains(String userId) {
    return getPrincipals().contains(userId);
  }
}
