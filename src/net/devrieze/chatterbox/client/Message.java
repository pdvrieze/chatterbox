package net.devrieze.chatterbox.client;

import com.google.gwt.xml.client.NodeList;


public class Message {

  private final int index;
  private final long epoch;
  private final NodeList content;

  public Message(String index, String epoch, NodeList content) {
    this.index = Integer.parseInt(index);
    if (this.index<0) {
      throw new IllegalArgumentException("Index can not be smaller than zero");
    }
    this.epoch = Long.parseLong(epoch);
    this.content = content;
  }

  public Message(int index, long epoch, NodeList content) {
    this.index = index;
    if (this.index<0) {
      throw new IllegalArgumentException("Index can not be smaller than zero");
    }
    this.epoch = epoch;
    this.content = content;
  }

  public int getIndex() {
    return index;
  }

  public long getEpoch() {
    return epoch;
  }

  public NodeList getContent() {
    return content;
  }

}
