package net.devrieze.chatterbox.server;

import java.util.HashMap;
import java.util.Map;

public final class Util {

  private static enum SafeTag {
    ABBR("abbr"),
    ACRONYM("acronym"),
    ADDRESS("address"),
    B("b"),
    BIG("big"),
    BLOCKQUOTE("blockquote"),
    BR("br"),
    CENTER("center"),
    CITE("cite"),
    CODE("code"),
    DD("dd"),
    DEL("del"),
    DFN("dfn"),
    DIR("dir"),
    DIV("div"),
    DL("dl"),
    DT("dt"),
    EM("em"),
    H1("h1"),
    H2("h2"),
    H3("h3"),
    H4("h4"),
    H5("h5"),
    H6("h6"),
    HR("hr"),
    I("i"),
    INS("ins"),
    KBD("kbd"),
    LI("li"),
    MENU("menu"),
    OL("ol"),
    P("p"),
    PRE("pre"),
    Q("q"),
    S("s"),
    SAMP("samp"),
    SMALL("small"),
    SPAN("span"),
    STRIKE("strike"),
    STRONG("strong"),
    SUB("sub"),
    SUP("sup"),
    TABLE("table"),
    TBODY("tbody"),
    TD("td"),
    TFOOT("tfoot"), 
    TH("th"),
    THEAD("thead"),
    TR("tr"),
    U("u"),
    UL("ul"),
    VAR("var"),
    ;
    
    private String tagname;

    SafeTag(String tagname) {
      this.tagname = tagname;
    }
    
    public String getTagname() {
      return tagname;
    }
  }

//  private static final String[] ALLOWEDTAGNAMES = { "b", "i", "p", "div", "span", "code", "br", "ul", "em", "strong", "cite", "dfn",
//                                                   "code", "samp", "kbd", "var", "abbr", "acronym", "blockquote", "q", "pre", "ins", "del",
//                                                   "h1", "h2", "h3", "h4", "h5", "h6", "table", "th", "td", "tr", "tbody", "thead", "tfoot" };

  private static final String[] ALLOWEDENTITIES = { "lt", "gt", "quot", "amp", "nbsp" };
  private static Map<String,SafeTag> safeTagMap;

  private Util() {}

  public static String encodeHtml(CharSequence source) {
    if (source == null) {
      return null;
    }
    StringBuilder result = null;
    char c;
    String repl = null;
    for (int i = 0; i < source.length(); ++i) {
      c = source.charAt(i);
      switch (c) {
        case '&':
          repl = "&amp;";
          break;
        case '"':
          repl = "&quot;";
          break;
        case '<':
          repl = "&quot;";
          break;
        case '>':
          repl = "&quot;";
          break;
        default:
          repl = null;
      }
      if (repl != null) {
        if (result == null) {
          result = new StringBuilder((source.length() * 3) / 2);
          result.append(source.subSequence(0, i));
        }
        result.append(repl);
      } else if (result != null) {
        result.append(c);
      }
    }
    if (result == null) {
      return source.toString();
    }
    return result.toString();
  }

  public static String sanitizeHtml(CharSequence orig) {
    if (orig == null) {
      return null;
    }
    try {
      StringBuilder result = new StringBuilder(orig.length());
      sanitizeHtml(0, result, null, orig);
      return result.toString();
    } catch (RuntimeException e) {
      System.err.println("Error sanitizing HTML, just encoding the lot now: " + e.getMessage());
      e.printStackTrace();
      return encodeHtml(orig);
    }
  }

  private static int sanitizeHtml(int startPos, StringBuilder result, String closeTag, CharSequence orig) {
    int pos = startPos;
    char c;
    for (pos = startPos; pos < orig.length(); ++pos) {
      c = orig.charAt(pos);
      switch (c) {
        case '<':
          pos = sanitizeTag(pos + 1, result, closeTag, orig);
          if (pos < 0) {
            return pos;
          }
          break;
        case '&':
          pos = sanitizeEntity(pos + 1, result, orig);
          break;
        case '"':
          result.append("&quot;");
          break;
        case '>':
          result.append("&gt;");
          break;
        default:
          result.append(c);
      }
    }
    return pos;
  }

  private static int sanitizeTag(int startPos, StringBuilder result, String closeTag, CharSequence orig) {
    int pos = startPos;
    char c;
    boolean endTag = false;
    if (startPos < orig.length() && orig.charAt(startPos) == '/') {
      endTag = true;
      ++pos;
    }
    for (; pos < orig.length(); ++pos) {
      c = orig.charAt(pos);
      if (Character.isLetter(c)) {
        // Do nothing
      } else {
        String tagName2 = orig.subSequence(startPos + (endTag ? 1 : 0), pos).toString();
        SafeTag tag = sanitizeTagName(tagName2);
        if (tag != null) {
          pos = clearToEndOfTag(pos, orig);
          if (endTag) {
            if (!tag.getTagname().equals(closeTag)) {
              result.append("&lt;");
              return startPos - 1;
            } else {
              result.append("</").append(tag.getTagname()).append('>');
              return -pos;
            }
          } else {
            if (orig.charAt(pos - 1) == '/') { // Self closing tag
              result.append("<").append(tag.getTagname()).append(" />");
              return pos;
            } else {
              result.append("<").append(tag.getTagname()).append('>');
            }
            pos = sanitizeHtml(pos + 1, result, tag.getTagname(), orig);
            if (pos < 0) {
              return -pos;
            } else {
              result.append("</").append(tag.getTagname()).append('>');
              return pos;
            }
          }
        } else {
          result.append("&lt;");
          return startPos - 1;
        }

      }
    }
    result.append("&lt;");
    return startPos - 1;
  }

  private static SafeTag sanitizeTagName(String tagName) {
    String u = tagName.toLowerCase();
    if (safeTagMap==null) {
      getSafeTagMap();
    }
    SafeTag t = safeTagMap.get(u);
    return t;
  }

  private synchronized static void getSafeTagMap() {
    if (safeTagMap==null) {
      safeTagMap = new HashMap<String, Util.SafeTag>();
      for(SafeTag t:SafeTag.values()) {
        safeTagMap.put(t.getTagname(), t);
      }
    }
    // TODO Auto-generated method stub
    
  }

  private static int clearToEndOfTag(int startPos, CharSequence orig) {
    int pos;

    for (pos = startPos; pos < orig.length(); ++pos) {
      switch (orig.charAt(pos)) {
        case '"':
          pos = clearToChar(pos + 1, '"', orig);
          break;
        case '\'':
          pos = clearToChar(pos + 1, '\'', orig);
          break;
        case '>':
          return pos;
        default:
          // Do nothing
      }
    }
    return pos;
  }

  private static int clearToChar(int startPos, char match, CharSequence orig) {
    for (int pos = startPos; pos < orig.length(); ++pos) {
      if (orig.charAt(pos) == match)
        return pos;
    }
    return orig.length();
  }

  private static int sanitizeEntity(int startPos, StringBuilder result, CharSequence orig) {
    int pos = startPos;
    char c;
    for (pos = startPos; pos < orig.length(); ++pos) {
      c = orig.charAt(pos);
      if (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))) {
        // Just a letter
        ++pos;
        continue;
      } else if (c == ';') {
        String entity = orig.subSequence(startPos, pos).toString();
        for (String allowed : ALLOWEDENTITIES) {
          if (allowed.equals(entity)) {
            result.append('&').append(allowed).append(';');
            return pos;
          }
        }
      }
      // Not an allowed entity or not an allowed character
      break;
    }
    result.append("&amp;");
    return startPos - 1;
  }

}
