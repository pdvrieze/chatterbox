package net.devrieze.chatterbox.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class UtilTest {

  @Test
  public void testEncodeHtml() {
    assertEquals("hello&quot;&amp;blue;&amp;quot&amp;quot;", Util.encodeHtml("hello\"&blue;&quot&quot;"));
  }

  @Test
  public void testEncodeHtmlNull() {
    assertEquals(null, Util.encodeHtml(null));
  }

  @Test
  public void testSanitizeHtmlCharSequence1() {
    assertEquals("hello&quot;&amp;blue;&amp;quot&quot;", Util.sanitizeHtml("hello\"&blue;&quot&quot;"));
  }

  @Test
  public void testSanitizeHtmlCharSequence2() {
    assertEquals("<b>hello</b>", Util.sanitizeHtml("<b>hello</b>"));
  }

  @Test
  public void testSanitizeHtmlCharSequence3() {
    assertEquals("<b><i>hello</i></b>", Util.sanitizeHtml("<b><i>hello</i></b>"));
  }

  @Test
  public void testSanitizeHtmlCharSequence4() {
    assertEquals("<b>hello</b>", Util.sanitizeHtml("<b some garba83475ge*&)>hello</b>"));
  }

  @Test
  public void testSanitizeHtmlCharSequence5() {
    assertEquals("<b>&lt;/a&gt;&lt;a href=&quot;blabla&quot;hello</b>", Util.sanitizeHtml("<b></a><a href=\"blabla\"hello</b>"));
  }

  @Test
  public void testSanitizeHtmlCharSequence6() {
    assertEquals("<b>&lt;/a&gt;&lt;a href=&quot;blabla&quot;<i>hello&lt;/b&gt;</i></b>", Util.sanitizeHtml("<b></a><a href=\"blabla\"<i>hello</b>"));
  }

  @Test
  public void testSanitizeHtmlCharSequence7() {
    assertEquals("<b>&lt;/a&gt;&lt;a href=&quot;blabla&quot;<i>hello&lt;/b&gt;</i></b>", Util.sanitizeHtml("<b></a><a href=\"blabla\"<i>hello</b></i>"));
  }

  @Test
  public void testSanitizeHtmlCharSequence8() {
    assertEquals("<b>&lt;/a&gt;&lt;a href=&quot;blabla&quot;<i>hello</i></b>", Util.sanitizeHtml("<b></a><a href=\"blabla\"<i>hello</i></b>"));
  }

  @Test
  public void testSanitizeHtmlCharSequence9() {
    assertEquals("<b>&lt;a href=&quot;blabla&quot;&gt;<i>hello</i></b>", Util.sanitizeHtml("<b onload='doSomeNasty()'><a href=\"blabla\"><i>hello</i></b>"));
  }

  @Test
  public void testSanitizeHtmlCharSequence10() {
    assertEquals("<b>&lt;a href=&quot;blabla&quot;&gt;<i>hello<br /></i></b>", Util.sanitizeHtml("<b onload='doSomeNasty()'><a href=\"blabla\"><i>hello<br/></i></b>"));
  }

  @Test
  public void testSanitizeHtmlCharSequenceNull() {
    assertEquals(null, Util.sanitizeHtml(null));
  }

}
