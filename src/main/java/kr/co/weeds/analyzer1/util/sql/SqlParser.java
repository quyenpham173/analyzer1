package kr.co.weeds.analyzer1.util.sql;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import kr.co.weeds.analyzer1.model.SqlObject;
import org.apache.commons.lang3.StringUtils;

public class SqlParser {

   private SqlParser() {
   }

   private static final String SPACE_REGEX = "\\s+";
   private static final Pattern COMMA_PATTERN = Pattern.compile(",");
   private static final String SELECT = "SELECT ";
   private static final String INSERT = "INSERT INTO ";
   private static final String UPDATE = "UPDATE ";
   private static final String DELETE = "DELETE FROM ";
   private static final String[] SELECT_KEYWORDS = {
       "SELECT",
       "WHERE",
       "FROM",
       "GROUP",
       "ORDER"
   };

   public static void insertTables(Set<String> setTables, String tableName) {
      int idx = tableName.lastIndexOf('.');
      if (idx != -1) {
         if (idx == tableName.length() - 1) {
            return;
         }
         tableName = tableName.substring(idx + 1);
      }

      setTables.add(tableName);
   }

   public static SqlObject getSqlObjects(String sql) {
      try {
         sql = removeCommentsTuning(sql);
         if (StringUtils.isBlank(sql)) {
            return SqlObject.EMPTY_SQLOBJECT;
         }

         SqlObject result = new SqlObject();
         sql = sql.trim().toUpperCase().replaceAll(SPACE_REGEX, " ");
         Set<String> setTables = new HashSet<>();
         char[] sqlChars = sql.toCharArray();

         processBySqlAction(sql, result, setTables, sqlChars);

         String[] resultTables = new String[setTables.size()];
         setTables.toArray(resultTables);

         result.setTables(resultTables);
         result.setRemodelledSQL(sql);

         return result;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   private static void processBySqlAction(String sql, SqlObject result, Set<String> setTables, char[] sqlChars) {
      int idx;
      int lastIdx = 0;
      if (sql.startsWith(SELECT)) {
         result.setAction(SqlObject.ACTION_SELECT);
         while ((idx = sql.indexOf(" FROM ", lastIdx)) >= 0) {
            int start = idx + 6;
            lastIdx = getSelectTables(setTables, sql, sqlChars, start);
         }
      } else if (sql.startsWith(INSERT)) {
         processActionInsert(sql, result, setTables);
      } else if (sql.startsWith(UPDATE)) {
         processActionUpdate(sql, result, setTables);

      } else if (sql.startsWith(DELETE)) {
         processActionDelete(sql, result, setTables);
      } else {
         processActionDefault(sql, result);
      }
   }

   private static void processActionUpdate(String sql, SqlObject result, Set<String> setTables) {
      int idx;
      int lastIdx;
      result.setAction(SqlObject.ACTION_UPDATE);
      idx = UPDATE.length();
      lastIdx = sql.indexOf(' ', UPDATE.length());
      if (lastIdx == -1) {
         lastIdx = sql.length();
      }
      insertTables(setTables, sql.substring(idx, lastIdx).trim());
   }

   private static void processActionDelete(String sql, SqlObject result, Set<String> setTables) {
      int lastIdx;
      int idx;
      result.setAction(SqlObject.ACTION_DELETE);
      idx = DELETE.length();
      lastIdx = sql.indexOf(' ', DELETE.length());
      if (lastIdx == -1) {
         lastIdx = sql.length();
      }
      insertTables(setTables, sql.substring(idx, lastIdx).trim());
   }

   private static void processActionDefault(String sql, SqlObject result) {
      int idx;
      int lastIdx;
      idx = sql.indexOf(' ');
      if (idx == -1) {
         result.setAction(sql);
      } else {
         lastIdx = sql.indexOf(' ', idx + 1);
         if (lastIdx == -1) {
            result.setAction(sql.substring(0, idx));
         } else {
            result.setAction(sql.substring(0, lastIdx));
         }
      }
   }

   private static void processActionInsert(String sql, SqlObject result, Set<String> setTables) {
      int idx;
      int lastIdx;
      result.setAction(SqlObject.ACTION_INSERT);
      idx = INSERT.length();
      lastIdx = sql.indexOf(' ', INSERT.length());
      int lastIdx2 = sql.indexOf('(', INSERT.length());
      int valueIdx1 = sql.indexOf("VALUE", INSERT.length());
      int valueIdx2 = sql.indexOf("SELECT", INSERT.length());
      int valueIdx = -1;
      if (valueIdx1 != -1) {
         valueIdx = valueIdx1;
      } else if (valueIdx2 != -1) {
         valueIdx = valueIdx2;
      }
      if (lastIdx == -1) {
         if (lastIdx2 != -1 && lastIdx2 < valueIdx) {
            lastIdx = lastIdx2;
         } else if (lastIdx2 == -1 && valueIdx == -1) {
            lastIdx = sql.length();
         } else if (lastIdx2 == -1) {
            lastIdx = valueIdx;
         } else {
            lastIdx = sql.length();
         }
      } else {
         if (lastIdx2 != -1 && lastIdx2 < lastIdx) {
            lastIdx = lastIdx2;
         }
      }
      insertTables(setTables, sql.substring(idx, lastIdx).trim());
   }

   private static boolean isSelectKeyword(String sql, int idx) {
      return Arrays.stream(SELECT_KEYWORDS).anyMatch(selectKeyword -> sql.startsWith(selectKeyword, idx));
   }

   private static int getSelectTables(Set<String> setTables, String sql, char[] sqlChars, int idx) {
      String tableBlock;
      int charIndex;
      for (charIndex = idx; charIndex < sqlChars.length; charIndex++) {
         if (sqlChars[charIndex] == '(' || sqlChars[charIndex] == ')' || (isSelectKeyword(sql, charIndex))) {
            break;
         }
      }

      int lastIndex = charIndex;
      tableBlock = sql.substring(idx, lastIndex);
      if (StringUtils.isBlank(tableBlock)) {
         return lastIndex;
      }

      tableBlock = tableBlock.trim().replace(" JOIN ", ",");
      String[] tables = COMMA_PATTERN.split(tableBlock);
      for (charIndex = 0; charIndex < tables.length; charIndex++) {
         tables[charIndex] = tables[charIndex].trim();
         int sidx = tables[charIndex].indexOf(" ");
         if (sidx == -1) {
            sidx = tables[charIndex].length();
         }
         String table = tables[charIndex].substring(0, sidx);
         if (StringUtils.isBlank(table)) {
            continue;
         }
         insertTables(setTables, table.trim());
      }

      return lastIndex;
   }

   private static String removeCommentsTuning(String sql) {
      char[] prgmChars = sql.toCharArray();
      int n = prgmChars.length;

      StringBuilder res = new StringBuilder(n);
      boolean isSingleComment = false;
      boolean isMultiComment = false;

      // Traverse the given program
      for (int i = 0; i < n; i++) {
         // If single line comment flag is on, then check for end of it
         if (isSingleComment && prgmChars[i] == '\n') {
            isSingleComment = false;
         }

         // If multiple line comment is on, then check for end of it
         else if (isMultiComment && prgmChars[i] == '*' && i + 1 < n && prgmChars[i + 1] == '/') {
            isMultiComment = false;
            i++;
         }

         // If this character is in a comment, ignore it
         else if (isSingleComment || isMultiComment) {
            continue;
         }

         // Check for beginning of comments and set the approproate flags
         else if (prgmChars[i] == '-' && i + 1 < n && prgmChars[i + 1] == '-') {
            isSingleComment = true;
            i++;
         } else if (prgmChars[i] == '/' && i + 1 < n && prgmChars[i + 1] == '*') {
            isMultiComment = true;
            i++;
         }

         // If current character is a non-comment character, append it to res
         else {
            res.append(prgmChars[i]);
         }
      }
      return res.toString();
   }


}
