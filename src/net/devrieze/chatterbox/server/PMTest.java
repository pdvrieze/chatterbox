package net.devrieze.chatterbox.server;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION, detachable="true")
public class PMTest {
  
  @PrimaryKey
  @Persistent
  private String key = "test";
  
  @Persistent
  private String value = "val";
  
  public String getKey() {
    return key;
  }
  
  public void setKey(String pKey) {
    key = pKey;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

}
