package kr.co.weeds.analyzer1.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.co.weeds.analyzer1.model.LoaderInfo;
import kr.co.weeds.analyzer1.model.loader.AppInfo;
import kr.co.weeds.analyzer1.model.loader.SystemFamily;
import kr.co.weeds.analyzer1.model.loader.SystemInfo;
import kr.co.weeds.analyzer1.model.loader.TraceInfo;
import kr.co.weeds.analyzer1.model.loader.TraceLoginInfo;
import kr.co.weeds.analyzer1.repository.mybatis.mapper.AppInfoMapper;
import kr.co.weeds.analyzer1.repository.mybatis.mapper.SystemInfoMapper;
import kr.co.weeds.analyzer1.repository.mybatis.mapper.TraceInfoMapper;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TraceLoader {

   private final TraceInfoMapper traceInfoMapper;

   private final SystemInfoMapper systemInfoMapper;

   private final AppInfoMapper appInfoMapper;

   public LoaderInfo getLoaderInfo() {
      List<TraceInfo> traceInfo = getTraceInfo();
      List<SystemInfo> systemInfo = getSystemInfo();
      List<TraceLoginInfo> traceLoginInfo = getTraceLoginInfo();
      Map<String, List<String>> systemFamilyList = getSystemFamilyList();
      List<AppInfo> appInfoList = appInfoMapper.getAppInfoList();
      return new LoaderInfo(traceInfo, systemInfo, traceLoginInfo, systemFamilyList, appInfoList);
   }

   private List<TraceInfo> getTraceInfo() {
      return traceInfoMapper.getTraceInfoList();
   }

   private List<SystemInfo> getSystemInfo() {
      return systemInfoMapper.getSystemInfoList();
   }

   private List<TraceLoginInfo> getTraceLoginInfo() {
      return traceInfoMapper.getTraceLoginInfoList();
   }

   private Map<String, List<String>> getSystemFamilyList() {
      Map<String, List<String>> result = new HashMap<>();
      List<SystemFamily> systemFamilyList = systemInfoMapper.getSystemFamilyList();
      if (CollectionUtils.isNotEmpty(systemFamilyList)) {
         systemFamilyList.forEach(system -> {
            List<String> systems = new ArrayList<>();
            addSystem(systems, system.getSysId1());
            addSystem(systems, system.getSysId2());
            addSystem(systems, system.getSysId3());
            addSystem(systems, system.getSysId4());
            addSystem(systems, system.getSysId5());
            result.put(system.getTraceId(), systems);
         });
      }
      return result;
   }

   private void addSystem(List<String> systems, String systemId) {
      if (StringUtils.isNotBlank(systemId)) {
         systems.add(systemId);
      }
   }

}
