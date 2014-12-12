package wse.ne.reco;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

/*
 * Tokenize the raw text to build the top term list for the text
 */

public class Tokenization {
  // decide whether a character is a number 
  private static boolean isCharacter(char c) {
    if ((c >= 'a' && c <= 'z') || 
        (c >= 'A' && c <= 'Z') || 
        c == '-') {
      return true;
    } else {
      return false;
    }
  }
  
  private static String prune(String term) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (i < term.length() && isCharacter(term.charAt(i))) {
      sb.append(term.charAt(i));
    }
    
    if (sb.capacity() == 0) {
      return null;
    } else {
      return new String(sb);
    }
  }
  
  // tokenizer for the input string
  public static Map<String, Integer> Tokenization(String text) {
    Vector<String> r = new Vector<String>();
    Map<String, Integer> map = new HashMap<String, Integer>();
    if (text.length() == 0 || text == null) {
      return map;
    }
    
    Scanner s = new Scanner(text);
    while (s.hasNext()) {
      String temp = prune(s.next().toLowerCase());
      if (temp != null && !StopWordsList.isStopWord(temp)) {
        r.add(temp);
      }
    }
    
    return map;
  }
}
