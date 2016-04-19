package net.devrieze.chatterbox.client;

import net.devrieze.chatterbox.shared.MessagePojo;

import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;


public class Message extends MessagePojo{

  private static final long serialVersionUID = -24721040835336897L;

  public Message(MessagePojo pPojo) {
    super(pPojo);
  }

  /** @deprecated Does not record sender */
  @Deprecated
  public Message(String index, String epoch, NodeList content) {
    this(index, "nobody@example.com", epoch, content);
  }

  public Message(String index, String pSender, String epoch, NodeList content) {
    super(parseIndex(index), content.toString(), parseEpoch(epoch), pSender);
  }

  private static long parseIndex(String pIndex) {
    final long result = Long.parseLong(pIndex);
    if (result<0) {
      throw new IllegalArgumentException("Index can not be smaller than zero");
    }
    return result;
  }

  private static long parseEpoch(String pEpoch) {
    return Long.parseLong(pEpoch);
  }

  /** @deprecated Does not record sender */
  @Deprecated
  public Message(int index, long epoch, NodeList content) {
    super(index, "nobody@example.com", epoch, content.toString());
  }

  public Message(int index, String pSender, long epoch, NodeList content) {
    super(index, pSender, epoch, content.toString());
  }

  /** @deprecated This is inefficient, just set the content with setHtml */
  @Deprecated
  public NodeList getContent() {
    return XMLParser.parse("<r>"+getMessageBody()+"</r>").getDocumentElement().getFirstChild().getChildNodes();
  }

}
