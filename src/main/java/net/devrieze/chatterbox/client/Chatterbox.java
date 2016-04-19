package net.devrieze.chatterbox.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;


public class Chatterbox implements EntryPoint {

  @Override
  public void onModuleLoad() {
    RootPanel rootPanel = RootPanel.get();
    ChatterboxUI w = new ChatterboxUI();
    rootPanel.add(w);
    w.textBox.setFocus(true);
    w.textBox.selectAll();
//    Document d = Document.get();
//    d.getBody().getStyle().setFontSize(1.25, Unit.EM);
  }

}
