package kr.co.weeds.analyzer1.process.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kr.co.weeds.analyzer1.model.SqlObject;
import kr.co.weeds.analyzer1.util.sql.SqlParser;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class SystemTableObject {

   private final String traceId;
   private final Set<String> tableSet;
   private final String[] tables;

   public SystemTableObject(String traceId, String tables) {
      this.traceId = traceId;
      Set<String> tableSetData = new HashSet<>();
      List<String> tableListData = new ArrayList<>();

      String[] tables1 = tables.split(",");
      for (String table1 : tables1) {
         if (StringUtils.isNotBlank(table1)) {
            table1 = table1.trim().toUpperCase();
            if (tableSetData.contains(table1)) {
               continue;
            }
            tableSetData.add(table1);
            tableListData.add(table1);
         }
      }

      this.tableSet = tableSetData;
      this.tables = new String[tableListData.size()];
      tableListData.toArray(this.tables);
   }

   public boolean contains(String sql, SqlObject sqlObject) {
      if (sqlObject == null) {
         sqlObject = SqlParser.getSqlObjects(sql);
      }

      if (sqlObject != null && this.tableSet != null) {
         String[] tableArr = sqlObject.getTables();
         if (tableArr != null) {
            for (String table : tableArr) {
               if (this.tableSet.contains(table.toUpperCase())) {
                  return true;
               }
            }
         }
      }

      sql = sql.toUpperCase();
      for (String table : this.tables) {
         if (sql.contains(table)) {
            return true;
         }
      }
      return false;
   }

}
