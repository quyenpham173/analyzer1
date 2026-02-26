package kr.co.weeds.analyzer1.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SqlObject {

   public static final String[] EMPTY_TABLES = new String[0];
   public static final String EMPTY_SQL = "";
   public static final Map EMPTY_MAP = new HashMap();
   public static final SqlObject EMPTY_SQLOBJECT = new SqlObject();

   public static final String ACTION_SELECT = "SELECT";
   public static final String ACTION_INSERT = "INSERT";
   public static final String ACTION_UPDATE = "UPDATE";
   public static final String ACTION_DELETE = "DELETE";

   String action;
   String[] tables;
   Map columnAliasMap;
   String[] columnList;
   boolean isDML;
   String remodelledSQL;

   public void setAction(String action) {
      this.action = action;
      this.isDML = action != null &&
          (action.equals(ACTION_INSERT) || action.equals(ACTION_UPDATE) || action.equals(ACTION_DELETE));
   }

   public SqlObject() {
      action = EMPTY_SQL;
      tables = EMPTY_TABLES;
      columnAliasMap = EMPTY_MAP;
      isDML = false;
   }
}
