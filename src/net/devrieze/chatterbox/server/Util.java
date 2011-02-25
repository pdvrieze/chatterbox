package net.devrieze.chatterbox.server;


public final class Util {
  
  private static final String[] ALLOWEDTAGNAMES = { "b", "i", "p", "div", "span", "code", "br"};
  private static final String[] ALLOWEDENTITIES = { "lt", "gt", "quot", "amp"};

  private Util() {}

  public static String encodeHtml(String source) {
    StringBuilder result = null;
    char c;
    String repl = null;
    for(int i =0; i<source.length(); ++i) {
      c = source.charAt(i);
      switch (c) {
        case '&': repl="&amp;"; break;
        case '"': repl="&quot;"; break;
        case '<': repl="&quot;"; break;
        case '>': repl="&quot;"; break;
        default: repl=null;
      }
      if (repl!=null) {
        if (result==null) {
          result = new StringBuilder((source.length()*3)/2);
          result.append(source.substring(0, i));
        }
        result.append(repl);
      } else if (result!=null) { result.append(c); }
    }
    if (result==null) { return source; }
    return result.toString();
  }
  
  public static String sanitizeHtml(CharSequence orig) {
    StringBuilder result = new StringBuilder(orig.length());
    sanitizeHtml(0, result, null, orig);
    return result.toString();
  }
  
  private static int sanitizeHtml(int startPos, StringBuilder result, String closeTag, CharSequence orig) {
    int pos = startPos;
    char c;
    for(pos =startPos; pos<orig.length(); ++pos) {
      c = orig.charAt(pos);
      switch (c) {
        case '<':
          pos = sanitizeTag(pos+1, result, closeTag, orig);
          if (pos<0) {
            return pos;
          }
          break;
        case '&':
          pos = sanitizeEntity(pos+1, result, orig);
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
    if (startPos<orig.length()&& orig.charAt(startPos)=='/') {
      endTag = true;
      ++pos;
    }
    for(; pos<orig.length(); ++pos) {
      c = orig.charAt(pos);
      if (Character.isLetter(c)) {
        // Do nothing
      } else {
        String tagName = orig.subSequence(startPos+(endTag ? 1:0), pos).toString();
        tagName = sanitizeTagName(tagName);
        if (tagName!=null) {
          pos = clearToEndOfTag(pos, orig);
          if (endTag) {
            if (!tagName.equals(closeTag)) {
              result.append("&lt;");
              return startPos-1;
            } else {
              result.append("</").append(tagName).append('>');
              return -pos;
            }
          } else {
            if (orig.charAt(pos-1)=='/') { // Self closing tag
              result.append("<").append(tagName).append(" />");
            } else {
              result.append("<").append(tagName).append('>');
            }
            pos = sanitizeHtml(pos+1, result, tagName, orig);
            if (pos<0) { return -pos; } else {
              result.append("</").append(tagName).append('>');
              return pos;
            }
          }
        } else {
          result.append("&lt;");
          return startPos-1;
        }
        
      }
    }
    result.append("&lt;");
    return startPos-1;
  }

  private static String sanitizeTagName(String tagName) {
    for(String allowed: ALLOWEDTAGNAMES) {
      if (allowed.equals(tagName)) return allowed;
    }
    return null;
  }

  private static int clearToEndOfTag(int startPos, CharSequence orig) {
    int pos;
    
    for(pos =startPos; pos<orig.length(); ++pos) {
      switch (orig.charAt(pos)) {
        case '"': pos=clearToChar(pos+1, '"', orig); break;
        case '\'': pos=clearToChar(pos+1, '\'', orig); break;
        case '>': return pos;
        default:
          // Do nothing
      }
    }
    return pos;
  }

  private static int clearToChar(int startPos, char match, CharSequence orig) {
    for(int pos =startPos; pos<orig.length(); ++pos) {
      if (orig.charAt(pos)==match) return pos;
    }
    return orig.length();
  }

  private static int sanitizeEntity(int startPos, StringBuilder result, CharSequence orig) {
    int pos = startPos;
    char c;
    for(pos =startPos; pos<orig.length(); ++pos) {
      c = orig.charAt(pos);
      if (((c>='a') && (c<='z')) || ((c>='A') && (c<='Z'))) {
        // Just a letter
        ++pos;
        continue;
      } else if (c==';') {
        String entity = orig.subSequence(startPos, pos).toString();
        for(String allowed: ALLOWEDENTITIES) {
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
    return startPos-1;
  }

}
