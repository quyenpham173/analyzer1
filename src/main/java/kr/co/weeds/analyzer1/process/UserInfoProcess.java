package kr.co.weeds.analyzer1.process;

import static kr.co.weeds.analyzer1.process.function.FunctionProcess.getFromFunction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import kr.co.weeds.analyzer1.integration.OpUserAdapter;
import kr.co.weeds.analyzer1.model.LoaderInfo;
import kr.co.weeds.analyzer1.model.SqlObject;
import kr.co.weeds.analyzer1.model.ana1.LogRegisterModel;
import kr.co.weeds.analyzer1.model.ana1.UserInfo;
import kr.co.weeds.analyzer1.model.ana1.UserSystemInfo;
import kr.co.weeds.analyzer1.model.config.LogAnalysisConfig;
import kr.co.weeds.analyzer1.model.lh.LogHouseModel;
import kr.co.weeds.analyzer1.model.loader.OpUser;
import kr.co.weeds.analyzer1.model.loader.SystemInfo;
import kr.co.weeds.analyzer1.model.loader.TraceInfo;
import kr.co.weeds.analyzer1.model.loader.TraceLoginInfo;
import kr.co.weeds.analyzer1.process.config.SystemTableCheckConfig;
import kr.co.weeds.analyzer1.process.function.FunctionProcess;
import kr.co.weeds.analyzer1.repository.mybatis.mapper.SystemInfoMapper;
import kr.co.weeds.analyzer1.repository.mybatis.mapper.TraceInfoMapper;
import kr.co.weeds.analyzer1.util.CommonUtils;
import kr.co.weeds.analyzer1.util.sql.SqlParser;
import kr.co.weeds.dtos.log.sql.SqlLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import kr.co.weeds.dtos.NameValueObject;

@Service
@RequiredArgsConstructor
public class UserInfoProcess {

   private static final Logger LOGGER = LogManager.getLogger(UserInfoProcess.class);

   private static final String[] SECOND_ID_PREFIX = {
       "___WT_ID01___",
       "___WT_ID02___",
       "___WT_ID03___",
       "___WT_ID04___",
       "___WT_ID05___",
       "___WT_ID06___",
       "___WT_ID07___",
       "___WT_ID08___",
       "___WT_ID09___",
       "___WT_ID10___"
   };

   private final SystemInfoMapper systemInfoMapper;

   private final TraceInfoMapper traceInfoMapper;

   private final OpUserAdapter opUserAdapter;

   public LogRegisterModel analyze(LogHouseModel log, LogAnalysisConfig logAnalysisConfig, LoaderInfo loaderInfo) {
      LogRegisterModel logRegister = new LogRegisterModel();
      updateTraceId(log, logAnalysisConfig);

      List<TraceInfo> traceInfoList = loaderInfo.getTraceInfoList();
      List<TraceLoginInfo> traceLoginInfoList = loaderInfo.getTraceLoginInfoList();
      TraceInfo traceInfo = getTraceInfo(log, traceInfoList, loaderInfo.getSystemInfoList());
      if (traceInfo != null) {
         setRegisterData(log, logRegister, traceInfo);
      } else {
         LOGGER.warn("There is no trace info for trace ID {}. Skip this log.", log.getTraceId());
         return null;
      }
      String userId = getUserId(log, traceInfoList, traceLoginInfoList);
      if (StringUtils.isBlank(userId)) {
         userId = FunctionProcess.processNoLogin(log, userId, logAnalysisConfig, traceInfoList);
      }
      OpUser opUserInfo = null;
      UserInfo userInfo = logRegister.getUserInfo();
      if (StringUtils.isBlank(userId)) {
         String clientIp = userInfo.getClientIp();
         if (logAnalysisConfig.isUserMappingByIp() && StringUtils.isNotBlank(clientIp)) {
            opUserInfo = getOpUserInfo(null, null, clientIp);
         }
      } else {
         if (logAnalysisConfig.isMappingOpUserByOpUserid()) {
            String mappingOpUserByOpUseridPrefix = logAnalysisConfig.getMappingOpUserByOpUseridPrefix();
            if (StringUtils.isNotBlank(mappingOpUserByOpUseridPrefix)) {
               userId = mappingOpUserByOpUseridPrefix + userId;
            }
            opUserInfo = getOpUserInfo(userId, null, null);
         } else {
            String parentTraceId = log.getParentTraceId();
            List<String> systemFamilyParent = null;
            if (StringUtils.isNotBlank(parentTraceId)) {
               systemFamilyParent = loaderInfo.getSystemFamily(parentTraceId);
            }
            List<String> systemFamilyList = loaderInfo.getSystemFamily(log.getTraceId());
            List<String> searchSystemIdList = new ArrayList<>();
            for (String sysId : systemFamilyList) {
               searchSystemIdList.add(sysId);
               for (String prefix : SECOND_ID_PREFIX) {
                  searchSystemIdList.add(prefix + sysId);
                  searchSystemIdList.add(prefix);
               }
            }
            for (String sysId : searchSystemIdList) {
               opUserInfo = getOpUserInfo(userId, sysId, null);
               if (opUserInfo != null) {
                  break;
               }
            }
            if (opUserInfo == null && CollectionUtils.isNotEmpty(systemFamilyParent)) {
               for (String sysId : systemFamilyParent) {
                  opUserInfo = getOpUserInfo(userId, sysId, null);
                  if (opUserInfo != null) {
                     break;
                  }
               }
            }
         }
      }
      userInfo.setUserId(userId);
      if (opUserInfo != null) {
         userInfo.setUserId(opUserInfo.getOpUserId());
         userInfo.setUserCode(opUserInfo.getOpUserCode());
         userInfo.setUserName(opUserInfo.getOpUserName());
         if (logAnalysisConfig.isClientIpFromOpUser() && StringUtils.isNotBlank(opUserInfo.getIp())) {
            userInfo.setClientIp(opUserInfo.getIp());
         }
         userInfo.setPosition(opUserInfo.getPosition());
         userInfo.setDuty(opUserInfo.getDuty());
         userInfo.setDeptInfo(opUserInfo.getDeptInfo());
      }
      return logRegister;
   }

   private static void updateTraceId(LogHouseModel log, LogAnalysisConfig logAnalysisConfig) {
      if (SystemTableCheckConfig.isCheck()) {
         List<SqlLog> sqlLogList = log.getSqlLogList();
         if (sqlLogList != null) {
            for (SqlLog sl : sqlLogList) {
               String sql = sl.getSql();
               String traceId = SystemTableCheckConfig.getTraceId(sql, SqlParser.getSqlObjects(sql));
               if (traceId != null) {
                  log.setTraceId(traceId);
                  LOGGER.info("[tableTraceIdCheck] [{}] ==> [{}]", sql, traceId);
                  break;
               }
            }
         }
      }
      if (logAnalysisConfig.isTableSystemMapUrlCheck()) {
         String sql = log.getUrl().trim();
         if (!sql.isEmpty()) {
            SqlObject sqlObject = SqlParser.getSqlObjects(sql);
            if (sqlObject != null) {
               String traceId = SystemTableCheckConfig.getTraceId(sql, sqlObject);
               if (traceId != null) {
                  log.setTraceId(traceId);
                  LOGGER.info("[tableTraceIdCheck from URL] [{}] ==> [{}]", sql, traceId);
               }
            }
         }
      }

      if (logAnalysisConfig.isUseParamSearch()) {
         Map<String, String> paramStringMap = logAnalysisConfig.getParamStringSystemMap();
         if (MapUtils.isNotEmpty(paramStringMap)) {
            String paramString = listToUriText(log.getListParameters());
            for (Entry<String, String> entry : paramStringMap.entrySet()) {
               String checkParam = entry.getKey();
               if (paramString.contains(checkParam)) {
                  String traceID = entry.getValue();
                  LOGGER.info("paramSearch INPUT STREAM keyword and set app server name code = {}", traceID);
                  log.setTraceId(traceID);
                  break;
               }
            }
         }
      }
   }

   private void setRegisterData(LogHouseModel log, LogRegisterModel logRegister, TraceInfo traceInfo) {
      UserSystemInfo systemInfo = new UserSystemInfo();
      systemInfo.setSystemId(traceInfo.getSystemId());
      systemInfo.setSystemName(traceInfo.getSystemName());
      systemInfo.setSystemType(traceInfo.getSystemType());
      systemInfo.setTraceId(traceInfo.getTraceId());
      systemInfo.setTraceName(traceInfo.getTraceName());
      systemInfo.setTraceType(traceInfo.getTraceType());
      systemInfo.setParentSysId(traceInfo.getParentSysId());
      systemInfo.setLogType(traceInfo.getLogType());
      UserInfo userInfo = new UserInfo();
      userInfo.setClientIp(log.getClientIp());
      userInfo.setUserId(StringUtils.EMPTY);
      userInfo.setUserCode(0);
      userInfo.setUserName(StringUtils.EMPTY);

      logRegister.setStartTime(log.getStartTime());
      logRegister.setCreatedTime(LocalDateTime.now());
      logRegister.setLogNum(log.getLogNum());
      logRegister.setLogId(log.getLogId());
      logRegister.setUserInfo(userInfo);
      logRegister.setSystemInfo(systemInfo);
   }

   private OpUser getOpUserInfo(String userId, String systemId, String clientIp) {
      MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
      if (StringUtils.isBlank(userId) && StringUtils.isNotBlank(clientIp)) {
         requestParams.add("ip", clientIp);
         return opUserAdapter.getOpUserInfo(requestParams, "/ip");
      } else if (StringUtils.isBlank(systemId)) {
         requestParams.add("opuser_id", userId);
         return opUserAdapter.getOpUserInfo(requestParams, "/id");
      } else {
         requestParams.add("system", systemId);
         requestParams.add("opuser_id", userId);
         return opUserAdapter.getOpUserInfo(requestParams, "/system/id");
      }
   }

   private TraceInfo getTraceInfo(LogHouseModel log, List<TraceInfo> traceInfoList,
       List<SystemInfo> systemInfoList) {
      for (TraceInfo tnTraceInfo : traceInfoList) {
         if (log.getTraceId().equals(tnTraceInfo.getTraceId())) {
            if (tnTraceInfo.getParentSysId() == null) {
               for (SystemInfo tnSysInfo : systemInfoList) {
                  if (tnSysInfo != null && tnSysInfo.getSystemId().equals(tnTraceInfo.getSystemId())) {
                     if (StringUtils.isNotEmpty(tnSysInfo.getSystemIdUp())) {
                        tnTraceInfo.setParentSysId(tnSysInfo.getSystemIdUp());
                     }
                     break;
                  }
               }
            }
            return tnTraceInfo;
         }
      }

      String traceId = log.getTraceId();
      TraceInfo parentTraceInfo = null;

      for (TraceInfo tnTraceInfo : traceInfoList) {
         if (tnTraceInfo != null && (log.getParentTraceId() != null && log.getParentTraceId()
             .equals(tnTraceInfo.getTraceId()))) {
            parentTraceInfo = tnTraceInfo;
            break;
         }
      }

      if (parentTraceInfo != null) {
         TraceInfo newTraceInfo = TraceInfo.newTraceInfoFromParent(traceId, parentTraceInfo);
         insertNewTraceInfo(traceInfoList, newTraceInfo, parentTraceInfo);
         newTraceInfo.setParentSysId(parentTraceInfo.getSystemId());
         return newTraceInfo;
      }
      return null;
   }

   private void insertNewTraceInfo(List<TraceInfo> traceInfoList, TraceInfo newTraceInfo, TraceInfo parentTraceInfo) {
      systemInfoMapper.insertNewSystemInfo(newTraceInfo.getSystemId(), parentTraceInfo.getSystemId());
      traceInfoMapper.insertNewTraceInfo(newTraceInfo, parentTraceInfo);
      traceInfoList.add(newTraceInfo);
   }

   private String getUserId(LogHouseModel log, List<TraceInfo> traceInfoList,
       List<TraceLoginInfo> traceLoginInfoList) {
      if ((log.getCode().startsWith("U") && StringUtils.isNotEmpty(log.getAdditionalData2())) || log.getCode()
          .equals("UL") || log.getCode().equals("SL")) {
         return log.getAdditionalData1();
      }

      for (TraceInfo traceInfo : traceInfoList) {
         TraceLoginInfo traceLoginInfo = getLoginUri(log, traceLoginInfoList);
         if (traceLoginInfo != null) {
            String userId = getLoginUserId(log, (traceLoginInfo).getUserParamName(),
                (traceLoginInfo).getUserParamFunc());
            if (StringUtils.isNotBlank(userId)) {
               return userId;
            }
         } else if (traceInfo.getTraceId() != null && log.getTraceId() != null && traceInfo.getTraceId()
             .equals(log.getTraceId())) {
            String userParamFunc = traceInfo.getUserParamFunc();
            if (StringUtils.isBlank(userParamFunc)) {
               return StringUtils.EMPTY;
            }

            String userId = getLoginUserId(log, userParamFunc, userParamFunc);
            if (StringUtils.isNotBlank(userId)) {
               return userId;
            }
         }
      }
      return StringUtils.EMPTY;
   }

   private TraceLoginInfo getLoginUri(LogHouseModel log, List<TraceLoginInfo> traceLoginInfoList) {
      for (TraceLoginInfo item : traceLoginInfoList) {
         if (isCorrectUri(log, item, item.getUriInfo(item.getLoginUrl()))) {
            return item;
         }
      }
      return null;
   }

   private boolean isCorrectUri(LogHouseModel log, TraceLoginInfo si, Map<String, Object> correctInfo) {
      if (!log.getTraceId().equals(si.getTraceId())) {
         return false;
      }
      if (MapUtils.isEmpty(correctInfo)) {
         return false;
      }

      String uri = log.getUrl();
      List<NameValueObject> params = log.getListParameters();
      int paramsSize = params == null ? 0 : params.size();
      String correctUri = (String) correctInfo.get("uri");
      String[] correctParams = (String[]) correctInfo.get("params");

      if (correctUri.equals("_WEEDS_NXL_")) {
         return true;
      }
      if (!uri.trim().equals(correctUri.trim())) {
         return false;
      }
      if (ArrayUtils.isEmpty(correctParams)) {
         return true;
      }

      boolean existParam = false;
      for (String correctParam : correctParams) {
         if (StringUtils.isNotBlank(correctParam)) {
            existParam = true;
            break;
         }
      }
      if (!existParam) {
         return true;
      }

      for (String correctParam : correctParams) {
         int j = 0;
         if (StringUtils.isBlank(correctParam)) {
            continue;
         }
         for (; j < paramsSize; j++) {
            String paramText = params.get(j).toString();
            if (paramText.equals(correctParam)) {
               break;
            }
         }
         if (j >= paramsSize) {
            return false;
         }
      }
      return true;
   }

   private String getLoginUserId(LogHouseModel log, String userParamName, String userParamFunc) {
      List<NameValueObject> listParameters = log.getListParameters();
      if (StringUtils.isNotBlank(userParamFunc) && userParamFunc.trim().startsWith("{{PRE}}")) {
         // 만약 함수가 추가 될 경우 {{PRE}}_함수명_value 형태로 사용하게 변경, 함수용 객체를 만들고 함수명에 따른 동작 구현
         int findIndex = Integer.parseInt(userParamFunc.split("_")[1]);
         int findCount = 0;
         String result = StringUtils.EMPTY;
         for (NameValueObject nvo : listParameters) {
            if (nvo.getName().equals(userParamName)) {
               findCount++;
               if (findCount == findIndex) {
                  result = nvo.getValue();
                  break;
               }
            }
         }
         return result;
      } else if (StringUtils.isBlank(userParamFunc) || userParamFunc.trim().startsWith("{{POST}}")) {
         String result = StringUtils.EMPTY;
         for (NameValueObject nvo : listParameters) {
            if (nvo.getName().equals(userParamName)) {
               result = nvo.getValue();
               break;
            }
         }
         if (FunctionProcess.processLoginPostTag(log, userParamFunc, result)) {
            return result;
         }
      } else {
         if (userParamFunc.equals("lge_test")) {
            return FunctionProcess.processLgeTest(log);
         } else if (userParamFunc.startsWith("retrieveFromSql")) {
            return getFromFunction("retrieveFromSql", log, userParamFunc, null);
         } //rdh 수정 끝
         else if (userParamFunc.startsWith("retrieveFromResultBySOAP")) {
            return getFromFunction("retrieveFromResultBySOAP", log, userParamFunc, null);
         } else {
            int idx = userParamFunc.indexOf(StringUtils.SPACE);
            String function;
            String param = StringUtils.EMPTY;
            if (idx == -1) {
               function = userParamFunc;
            } else {
               function = userParamFunc.substring(0, idx);
               param = userParamFunc.substring(idx + 1);
            }
            return userProcess(log, userParamName, function, param);
         }
      }
      return StringUtils.EMPTY;
   }

   private String userProcess(LogHouseModel log, String userParamName, String function, String param) {
      List<NameValueObject> listParameters = log.getListParameters();
      if (function.equals("replace")) {
         return processReplaceFunc(log, userParamName, param, listParameters);
      } else if (function.equals("get_login_check_kbi")) {
         if (listParameters.isEmpty()) {
            return null;
         }
         NameValueObject nvo = listParameters.get(0);
         String paramName = nvo.getName();
         String paramPattern = FunctionProcess.getLoginCheckKbiPattern(paramName);
         if (!param.equals(paramPattern)) {
            return null;
         }
         return FunctionProcess.getLoginCheckKbiUserId(paramName);
      } else {
         String result = null;
         for (NameValueObject nvo : listParameters) {
            if (nvo.getName().equals(userParamName)) {
               result = nvo.getValue();
            }
         }
         return result;
      }
   }

   private String processReplaceFunc(LogHouseModel log, String userParamName, String param,
       List<NameValueObject> listParameters) {
      String result = null;
      for (NameValueObject nvo : listParameters) {
         if (nvo.getName().equals(userParamName)) {
            result = nvo.getValue();
         }
      }
      if (StringUtils.isNotEmpty(result)) {
         String[] replaceObject = param.split(StringUtils.SPACE);
         for (String s : replaceObject) {
            result = getFromFunction("replace", log, s, null);
         }
      }
      return result;
   }

   private static String listToUriText(List<NameValueObject> listData) {
      StringBuilder params = new StringBuilder();
      if (listData == null) {
         return "";
      }
      int size = listData.size();
      for (int i = 0; i < size; i++) {
         NameValueObject nvo = listData.get(i);
         String nvt = CommonUtils.encodeUriString(nvo.getName()) + "=" + CommonUtils.encodeUriString(nvo.getValue());
         params.append(nvt);
         if (i + 1 < size) {
            params.append("&");
         }
      }
      return params.toString();
   }

}
