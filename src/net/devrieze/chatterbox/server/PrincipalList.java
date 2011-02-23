package net.devrieze.chatterbox.server;

import java.util.Set;
import java.util.TreeSet;

import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class PrincipalList {
  
  @NotPersistent
  public static final String DEFAULTKEY = "principalList";

  @PrimaryKey
  @Persistent
  private String key = DEFAULTKEY;
  
  @Persistent
  private Set<String> principals;

  public Set<String> getPrincipals() {
    if (principals==null) {
      principals = new TreeSet<String>(); 
    }
    return principals;
  }

  public boolean contains(String principal) {
    return getPrincipals().contains(principal);
  }
}
