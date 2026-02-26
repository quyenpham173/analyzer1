package kr.co.weeds.analyzer1.process.function;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import kr.co.weeds.dtos.ApLoginIdSeq;
import kr.co.weeds.dtos.NameValueObject;
import kr.co.weeds.dtos.log.BaseLog;
import kr.co.weeds.functions.CommonFunction;
import kr.co.weeds.functions.UserFunction;
import kr.co.weeds.analyzer1.model.config.LogAnalysisConfig;
import kr.co.weeds.analyzer1.model.lh.LogHouseModel;
import kr.co.weeds.analyzer1.model.loader.TraceInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FunctionProcess {

   private FunctionProcess() {
   }

   private static final Logger LOGGER = LogManager.getLogger(FunctionProcess.class);

   public static String processLgeTest(LogHouseModel log) {
      List<NameValueObject> listCookies = log.getListCookies2();
      for (NameValueObject listCookie : listCookies) {
         if (listCookie.getName().equals("ssolgenet")) {
            String[] values = listCookie.getValue().split("&");
            for (String string : values) {
               int aIdx = string.indexOf('=');
               if (aIdx == -1 || aIdx + 1 == string.length()) {
                  continue;
               }
               String name = string.substring(0, aIdx);
               String value = string.substring(aIdx + 1);
               if (name.equals("ssoid")) {
                  return value;
               }
            }
            return "";
         }
      }
      return null;
   }

   public static String getFromFunction(String functionStr, LogHouseModel log, String param, String userId) {
      String className = StringUtils.capitalize(functionStr);
      try {
         Class<?> clazz = Class.forName("kr.co.weeds.functions.users." + className);
         UserFunction instance = (UserFunction) clazz.getDeclaredConstructor().newInstance();
         ApLoginIdSeq apLoginIdSeq = new ApLoginIdSeq();
         apLoginIdSeq.setUserId(userId);
         BaseLog baseLog = getBaseLog(log);
         instance.processUserId(apLoginIdSeq, param, baseLog);
         userId = apLoginIdSeq.getUserId();
      } catch (Exception e) {
         try {
            Class<?> clazz = Class.forName("kr.co.weeds.functions.common." + className);
            CommonFunction instance = (CommonFunction) clazz.getDeclaredConstructor().newInstance();
            userId = instance.get(userId, param);
         } catch (Exception ex) {
            LOGGER.error("Cannot create instance for function {}.", functionStr, ex);
         }
      }
      return userId;
   }

   public static boolean processLoginPostTag(LogHouseModel log, String userParamFunc, String result) {
      if (result != null) {
         String func = null;
         if (userParamFunc != null && userParamFunc.length() > 8) {
            func = userParamFunc.substring(8);
         }
         if (StringUtils.isNotEmpty(func)) {
            String[] functions = func.split(StringUtils.SPACE);
            for (String function : functions) {
               function = URLDecoder.decode(function, StandardCharsets.UTF_8);

               int paramIdx = function.indexOf(":");
               String functionStr;
               String param;
               if (paramIdx == -1) {
                  functionStr = function;
                  param = null;
               } else {
                  functionStr = function.substring(0, paramIdx);
                  if (paramIdx == (function.length() - 1)) {
                     param = null;
                  } else {
                     param = function.substring(paramIdx + 1);
                  }
               }

               result = getFromFunction(functionStr, log, param, result);
            }
         }
         return true;
      }
      return false;
   }

   public static String getLoginCheckKbiPattern(String p) {
      return p.replaceAll("'[a-zA-Z0-9_-]+'", "").replaceAll("[ 0-9,=()\".]+", "");
   }

   public static String getLoginCheckKbiUserId(String p) {
      String pp = p.replaceAll("[ ]+=[ ]+", "=");
      final String IDP = "WHERE I_ID='";
      int idx = pp.indexOf(IDP);
      if (idx > 0) {
         int lastIdx = pp.indexOf("'", idx + IDP.length());
         if (lastIdx > 0) {
            return pp.substring(idx + IDP.length(), lastIdx);
         }
      }
      return null;
   }

   public static String processNoLogin(LogHouseModel log, String userId, LogAnalysisConfig config,
       List<TraceInfo> traceInfoList) {
      String traceId = log.getTraceId();

      if (traceId.isEmpty()) {
         return StringUtils.EMPTY;
      }

      if (config.isForceCopyTraceIdFromParent()) {
         String parentTraceID = log.getParentTraceId();
         if (parentTraceID != null) {
            traceId = parentTraceID;
         }
      }

      TraceInfo traceInfo = null;
      for (TraceInfo trace : traceInfoList) {
         if (trace.getTraceId().equals(traceId)) {
            traceInfo = trace;
         }
      }
      if (traceInfo == null) {
         return StringUtils.EMPTY;
      }

      String userParamFunc = traceInfo.getUserParamFunc();
      if (StringUtils.isBlank(userParamFunc)) {
         return StringUtils.EMPTY;
      }

      String[] functions = userParamFunc.trim().split(StringUtils.SPACE);
      for (String function : functions) {
         function = URLDecoder.decode(function, StandardCharsets.UTF_8);

         int paramIdx = function.indexOf(":");
         String functionStr;
         String param;
         if (paramIdx == -1) {
            functionStr = function;
            param = null;
         } else {
            functionStr = function.substring(0, paramIdx);
            if (paramIdx == (function.length() - 1)) {
               param = null;
            } else {
               param = function.substring(paramIdx + 1);
            }
         }

         userId = getFromFunction(functionStr, log, param, userId);
      }
      return userId;
   }

   private static BaseLog getBaseLog(LogHouseModel log) {
      BaseLog baseLog = new BaseLog();
      baseLog.setStartTime(Date.from(log.getStartTime().atZone(ZoneId.systemDefault()).toInstant()));
      baseLog.setTraceID(log.getTraceId());
      baseLog.setClientIp(log.getClientIp());
      baseLog.setUrl(log.getUrl());
      baseLog.setParameterObjects(log.getListParameters());
      baseLog.setSession1Objects(log.getListSessions1());
      baseLog.setSession2Objects(log.getListSessions2());
      baseLog.setCookie1Objects(log.getListCookies1());
      baseLog.setCookie2Objects(log.getListCookies2());
      baseLog.setHeader1Lists(log.getListHeaders1());
      baseLog.setHeader2Lists(log.getListHeaders2());
      String paramSrc = log.getListParameters().stream()
          .map(parameter -> parameter.getName() + "=" + parameter.getValue())
          .collect(Collectors.joining("&"));
      baseLog.setParameterSrc(paramSrc);
      baseLog.setSqlLogs(log.getSqlLogList());
      return baseLog;
   }

}
