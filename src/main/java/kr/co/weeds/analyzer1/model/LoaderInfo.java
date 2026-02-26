package kr.co.weeds.analyzer1.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import kr.co.weeds.analyzer1.model.loader.AppInfo;
import kr.co.weeds.analyzer1.model.loader.SystemInfo;
import kr.co.weeds.analyzer1.model.loader.TraceInfo;
import kr.co.weeds.analyzer1.model.loader.TraceLoginInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoaderInfo {

   private List<TraceInfo> traceInfoList;

   private List<SystemInfo> systemInfoList;

   private List<TraceLoginInfo> traceLoginInfoList;

   private Map<String,List<String>> systemFamilyList;

   private List<AppInfo> appInfoList;

   public List<String> getSystemFamily(String traceID) {
      if(StringUtils.isEmpty(traceID) || MapUtils.isEmpty(systemFamilyList)) {
         return Collections.emptyList();
      }

      return systemFamilyList.get(traceID);
   }

}
