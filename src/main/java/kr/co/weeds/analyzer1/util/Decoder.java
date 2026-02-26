package kr.co.weeds.analyzer1.util;

public class Decoder {

   private Decoder() {
   }

   public static byte hexToByte(char c1, char c2) {
      c1 = hexToNibble(c1);
      c2 = hexToNibble(c2);
      return (byte) ((c1 << 4) & 0x0F0 | c2 & 0x0F);
   }

   public static char hexToNibble(char c) {
      if ((c >= '0') && (c <= '9')) {
         return (char) (c - '0');
      }
      if ((c >= 'a') && (c <= 'z')) {
         return (char) (c - 'a' + 10);
      }
      if ((c >= 'A') && (c <= 'Z')) {
         return (char) (c - 'A' + 10);
      }
      return '\000';
   }

}
