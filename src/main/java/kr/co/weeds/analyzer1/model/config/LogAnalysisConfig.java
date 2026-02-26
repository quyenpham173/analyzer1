package kr.co.weeds.analyzer1.model.config;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import kr.co.weeds.dtos.NameValueObject;
import kr.co.weeds.analyzer1.entity.postgres.TraceAnalyzeConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LogAnalysisConfig {

   private static final Logger LOGGER = LogManager.getLogger(LogAnalysisConfig.class);

   private String remodelProc;

   private String sessionIdFromCookie;

   private List<String> exceptIps;

   private boolean ipUnique;

   private boolean ipUniqueFirst;

   private boolean ipUniqueLast;

   private boolean tableSystemMapUrlCheck;

   private boolean mappingOpUserByOpUserid;

   private String mappingOpUserByOpUseridPrefix;

   private boolean userMappingByIp;

   private boolean useParamSearch;

   private boolean forceCopyTraceIdFromParent;

   private int ipUniquePos = -1;

   private String inputStreamParserClass;

   private List<String> paramValueWhiteList;

   private Map<String, String> paramStringSystemMap;

   private boolean paramContainsCheck;

   private boolean clientIpFromOpUser;

   public static LogAnalysisConfig loadConfig(TraceAnalyzeConfig traceAnalyzeConfig) {
      String configuration = traceAnalyzeConfig.getConfiguration();
      if (StringUtils.isBlank(configuration)) {
         return null;
      }
      try {
         Properties props = new Properties();
         props.load(new StringReader(configuration));
         LogAnalysisConfig config = new LogAnalysisConfig();
         config.setRemodelProc(props.getProperty("remodel.proc.class"));
         String setSessionIdFromCookie = props.getProperty("set.sessionId.from.cookie");
         if (StringUtils.isNotBlank(setSessionIdFromCookie)) {
            config.setSessionIdFromCookie(setSessionIdFromCookie.trim());
         }
         config.setTableSystemMapUrlCheck("Y".equals(props.getProperty("table.systemmap.urlcheck")));
         config.setMappingOpUserByOpUserid("Y".equals(props.getProperty("mapping.opuser.by.opuserid")));
         config.setMappingOpUserByOpUseridPrefix(props.getProperty("mapping.opuser.by.opuserid.prefix"));
         config.setUserMappingByIp("Y".equals(props.getProperty("user.mapping.by.ip")));
         config.setForceCopyTraceIdFromParent("Y".equals(props.getProperty("force.copy.traceId.from.parent")));
         config.setUseParamSearch("Y".equals(props.getProperty("param_string.mode")));
         config.setParamContainsCheck("Y".equals(props.getProperty("screen.map[params].contains.check")));
         config.setClientIpFromOpUser("Y".equals(props.getProperty("set.clientIp.from.opuser")));

         setExceptIp(props, config);
         setIpUnique(props, config);

         Map<String, List<ParamMatchingTraceInfo>> paramMatchingMap = new LinkedHashMap<>();
         List<String> paramValueWhiteList = new ArrayList<>();
         Map<String, String> paramStringMap = new LinkedHashMap<>();
         Set<Object> keySet = props.keySet();
         for (Object o : keySet) {
            String key = (String) o;
            String value = props.getProperty(key);

            if (key.startsWith("param.matching.systemmap[")) {
               int idx = "param.matching.systemmap[".length();
               int endIdx;
               endIdx = key.indexOf("]", idx);
               if (endIdx == -1) {
                  endIdx = key.length();
               }
               String paramNameValue = key.substring(idx, endIdx);
               if (paramNameValue.isEmpty()) {
                  continue;
               }
               NameValueObject paramNVO = new NameValueObject(paramNameValue);
               if (paramNVO.getValue().isEmpty()) {
                  continue;
               }
               MatchingObject mo = MatchingObject.set("K:" + paramNVO.getValue());
               if (mo == null) {
                  continue;
               }
               if (!mo.isEqual()) {
                  List<ParamMatchingTraceInfo> listMO = paramMatchingMap.computeIfAbsent(paramNVO.getName(),
                      k -> new ArrayList<>());
                  ParamMatchingTraceInfo pmti = new ParamMatchingTraceInfo();
                  pmti.setTraceId(value);
                  pmti.setMo(mo);
                  listMO.add(pmti);
               }
            } else if (key.startsWith("param_value.writeonly[")) {
               int idx = "param_value.writeonly[".length();
               int endIdx;
               endIdx = key.indexOf("]", idx);
               if (endIdx == -1) {
                  endIdx = key.length();
               }
               String url = key.substring(idx, endIdx);
               if (url.isEmpty()) {
                  continue;
               }
               paramValueWhiteList.add(value);
            } else if (key.startsWith("param_string.systemmap[")) {
               int idx = "param_string.systemmap[".length();
               int endIdx;
               endIdx = key.indexOf("]", idx);
               if (endIdx == -1) {
                  endIdx = key.length();
               }
               String item = key.substring(idx, endIdx);
               if (item.isEmpty()) {
                  continue;
               }
               paramStringMap.put(item, value);
            }
         }
         config.setParamValueWhiteList(paramValueWhiteList);
         config.setParamStringSystemMap(paramStringMap);
         return config;
      } catch (IOException e) {
         LOGGER.error("Cannot load trace config of traceID {}.", traceAnalyzeConfig.getTraceId(), e);
         return null;
      }
   }

   private static void setExceptIp(Properties props, LogAnalysisConfig config) {
      String exceptIpList = props.getProperty("ip.except");
      if (StringUtils.isNotEmpty(exceptIpList)) {
         String[] exceptIpsArray = exceptIpList.split(",");
         for (int i = 0; i < exceptIpsArray.length; i++) {
            exceptIpsArray[i] = exceptIpsArray[i].trim();
         }
         config.setExceptIps(Arrays.asList(exceptIpsArray));
      }
   }

   private static void setIpUnique(Properties props, LogAnalysisConfig config) {
      String isIpUnique = props.getProperty("ip.unique");
      if (isIpUnique == null) {
         config.setIpUnique(false);
         config.setIpUniqueFirst(false);
         config.setIpUniqueLast(false);
         config.setIpUniquePos(-1);
      } else {
         isIpUnique = isIpUnique.trim();
         if (isIpUnique.isEmpty()) {
            config.setIpUnique(false);
            config.setIpUniqueFirst(false);
            config.setIpUniqueLast(false);
            config.setIpUniquePos(-1);
         } else {
            if (isIpUnique.equals("F")) {
               config.setIpUnique(true);
               config.setIpUniqueFirst(true);
               config.setIpUniqueLast(false);
               config.setIpUniquePos(-1);
            } else if (isIpUnique.equals("L")) {
               config.setIpUnique(true);
               config.setIpUniqueFirst(false);
               config.setIpUniqueLast(true);
               config.setIpUniquePos(-1);
            } else {
               try {
                  int iu = Integer.parseInt(isIpUnique);
                  if (iu < 0) {
                     throw new Exception("ip unique value is invalid. (<0)");
                  }
                  config.setIpUnique(true);
                  config.setIpUniqueFirst(false);
                  config.setIpUniqueLast(false);
                  config.setIpUniquePos(iu);
               } catch (Exception e) {
                  LOGGER.error("Error when get ip unique from config.", e);
                  config.setIpUnique(false);
                  config.setIpUniqueFirst(false);
                  config.setIpUniqueLast(false);
                  config.setIpUniquePos(-1);
               }
            }
         }
      }
   }

}
