package kr.co.weeds.analyzer1.process.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.co.weeds.analyzer1.model.SqlObject;
import lombok.Getter;

public class SystemTableCheckConfig {

	private SystemTableCheckConfig(){}

	private static final Map<String, SystemTableObject> SYSTEM_TABLE_MAP = new HashMap<>();
	private static final List<SystemTableObject> SYSTEM_TABLE_LIST = new ArrayList<>();

	@Getter
   static boolean check = false;
	
	public static void add(String traceId, String tables) {
		SystemTableObject sto = new SystemTableObject(traceId, tables);
		SYSTEM_TABLE_MAP.put(traceId, sto);
		SYSTEM_TABLE_LIST.add(sto);
		check = true;
	}

   public static String getTraceId(String sql, SqlObject sqlObject) {
		for(SystemTableObject sto : SYSTEM_TABLE_LIST) {
			if(sto.contains(sql, sqlObject)) {
				return sto.getTraceId();
			}
		}
		return null;
	}

}
