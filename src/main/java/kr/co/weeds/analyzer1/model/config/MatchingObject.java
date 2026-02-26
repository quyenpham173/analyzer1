package kr.co.weeds.analyzer1.model.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
@Setter
public class MatchingObject {

   private static final Logger LOGGER = LogManager.getLogger(MatchingObject.class);

   protected boolean prefixLike = false;
   protected boolean suffixLike = false;
   protected boolean isEqual = false;
   protected boolean isAll = false;
   protected String compareString = null;
   protected boolean regex = false;
   protected Pattern pattern = null;


   public String getNotMatchedTextOrEqual(String text) {
      if (regex) {
         return null;
      }
      if (isAll) {
         return "";
      }
      if (prefixLike && suffixLike) {
         int idx = text.indexOf(compareString);
         if (idx == -1) {
            return null;
         }
         return text.substring(0, idx) + text.substring(idx + compareString.length());
      }
      if (prefixLike) {
         int clength = compareString.length();
         int tlength = text.length();
         int idx = text.lastIndexOf(compareString);
         if (idx == -1 || idx != (tlength - clength)) {
            return null;
         }
         return text.substring(0, idx);
      }
      if (suffixLike) {
         int idx = text.indexOf(compareString);
         if (idx == -1) {
            return null;
         }
         return text.substring(0, compareString.length());
      }
      return compareString;
   }

   public static MatchingObject set(String pattern) {
      try {
         MatchingObject result = new MatchingObject();
         if (pattern == null) {
            return null;
         }
         int idx = pattern.indexOf(':');
         if (idx == -1) {
            return null;
         }
         String patternPrefix = pattern.substring(0, idx);
         String patternStr = pattern.substring(idx + 1);
         if (patternStr.isEmpty()) {
            return null;
         }
         if (patternPrefix.equals("R")) {
            result.prefixLike = false;
            result.suffixLike = false;
            result.isEqual = false;
            result.regex = true;
            result.compareString = patternStr;
            result.pattern = Pattern.compile(patternStr);
            result.isAll = false;
            return result;
         } else if (patternPrefix.equals("K")) {
            if (patternStr.equals("*")) {
               result.isEqual = false;
               result.regex = false;
               result.compareString = "";
               result.pattern = null;
               result.isAll = true;
               return result;
            }
            if (patternStr.startsWith("*")) {
               result.prefixLike = true;
               patternStr = patternStr.substring(1).trim();
            }
            if (patternStr.endsWith("*")) {
               result.suffixLike = true;
               patternStr = patternStr.substring(0, patternStr.length() - 1).trim();
            }
            result.isEqual = (!result.prefixLike) && (!result.suffixLike);
            result.regex = false;
            result.compareString = patternStr;
            result.pattern = null;
            result.isAll = false;
            return result;
         }
         return null;
      } catch (Exception e) {
         LOGGER.info("MatchingObject:set: {}", pattern);
         LOGGER.error("Error when set MatchingObject.", e);
         return null;
      }
   }

   public boolean match(String str) {
      try {
         str = str.trim();
         if (regex) {
            Matcher m = pattern.matcher(str);
            return m.find();
         }
         if (isAll) {
            return true;
         }
         if (isEqual) {
            return str.equals(compareString);
         }
         if (prefixLike && suffixLike) {
            return str.contains(compareString);
         }
         if (prefixLike) {
            return str.endsWith(compareString);
         }
         if (suffixLike) {
            return str.startsWith(compareString);
         }
         return str.equals(compareString);
      } catch (Exception e) {
         LOGGER.error("Error when check matching.", e);
         return false;
      }
   }

   public String toString() {
      return "compareString => " + compareString + ", P/S => (" + prefixLike + "," + suffixLike + ", regex ==> "
          + regex;
   }

   public static MatchingObject getMatches(String url, MatchingObject[] nuos) {
      if (nuos == null) {
         return null;
      }
      for (MatchingObject nuo : nuos) {
         if (nuo.match(url)) {
            return nuo;
         }
      }
      return null;
   }

}
