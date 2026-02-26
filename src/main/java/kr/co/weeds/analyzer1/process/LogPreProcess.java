package kr.co.weeds.analyzer1.process;

import java.util.ArrayList;
import java.util.List;
import kr.co.weeds.analyzer1.model.config.LogAnalysisConfig;
import kr.co.weeds.analyzer1.model.lh.LogHouseModel;
import kr.co.weeds.analyzer1.process.bfp.RemodelLogProc;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class LogPreProcess {

   private static final Logger LOGGER = LogManager.getLogger(LogPreProcess.class);

   public void remodel(LogHouseModel log, LogAnalysisConfig logAnalysisConfig) {
      String remodelProc = logAnalysisConfig.getRemodelProc();
      if (StringUtils.isNotBlank(remodelProc)) {
         try {
            Class<?> clazz = Class.forName(remodelProc);
            RemodelLogProc remodelLogProc = (RemodelLogProc) clazz.getDeclaredConstructor().newInstance();
            remodelLogProc.preRemodel(log);
         } catch (Exception e) {
            LOGGER.error("Error when remodel log data.", e);
         }
      }
      String clientIp = log.getClientIp();
      if (clientIp == null) {
         clientIp = "";
         log.setClientIp(clientIp);
      }
      clientIp = clientIp.trim();
      String[] ips = clientIp.split(",");
      if (ips.length == 1) {
         log.setClientIp(clientIp);
         return;
      }
      List<String> ipArray = new ArrayList<>();
      for (String ip : ips) {
         ip = ip.trim();
         if (!exceptIp(ip, logAnalysisConfig)) {
            ipArray.add(ip);
         }
      }
      ipArray = uniqueIpProcess(ipArray, logAnalysisConfig);
      String result = String.join(",", ipArray);
      log.setClientIp(result);
   }

//   public void setSessionId(LogHouseModel log, LogAnalysisConfig logAnalysisConfig) {
//      String setSessionIdFromCookie = logAnalysisConfig.getSessionIdFromCookie();
//      if (setSessionIdFromCookie != null) {
//         List<NameValueObject> cookieList = log.getListCookies1();
//         if (cookieList != null) {
//            for (NameValueObject cookie : cookieList) {
//               if (cookie.getName().trim().equals(setSessionIdFromCookie)) {
//                  log.setSessionId1(cookie.getValue());
//                  log.setSessionId2(cookie.getValue());
//                  break;
//               }
//            }
//         }
//         cookieList = log.getListCookies2();
//         if (cookieList != null) {
//            for (NameValueObject cookie : cookieList) {
//               if (cookie.getName().trim().equals(setSessionIdFromCookie)) {
//                  log.setSessionId1(cookie.getValue());
//                  log.setSessionId2(cookie.getValue());
//                  break;
//               }
//            }
//         }
//      }
//   }

   private boolean exceptIp(String ip, LogAnalysisConfig logAnalysisConfig) {
      List<String> exceptIps = logAnalysisConfig.getExceptIps();
      if (CollectionUtils.isEmpty(exceptIps)) {
         return false;
      }
      if (StringUtils.isBlank(ip)) {
         return true;
      }
      return exceptIps.contains(ip);
   }

   private List<String> uniqueIpProcess(List<String> ipArray, LogAnalysisConfig logAnalysisConfig) {
      if (!logAnalysisConfig.isIpUnique()) {
         return ipArray;
      }
      if (ipArray.isEmpty()) {
         return ipArray;
      }
      int pos;
      if (logAnalysisConfig.isIpUniqueFirst()) {
         pos = 0;
      } else if (logAnalysisConfig.isIpUniqueLast()) {
         pos = ipArray.size() - 1;
      } else {
         int size = ipArray.size();
         pos = logAnalysisConfig.getIpUniquePos();
         if (pos >= size) {
            pos = size - 1;
         }
      }
      List<String> result = new ArrayList<>();
      result.add(ipArray.get(pos));
      return result;
   }

}
