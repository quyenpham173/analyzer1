package kr.co.weeds.analyzer1.process.bfp;

import java.util.ArrayList;
import java.util.List;
import kr.co.weeds.analyzer1.model.lh.LogHouseModel;
import kr.co.weeds.analyzer1.util.CommonUtils;
import kr.co.weeds.dtos.NameValueObject;
import org.apache.commons.lang3.StringUtils;

public class BFPLogRemodel implements RemodelLogProc {

   private static final String DIFFGR = "</diffgr:diffgram>";

   @Override
   public void preRemodel(LogHouseModel log) {
      List<NameValueObject> result = new ArrayList<>();
      String parameterSrc = log.getParameterSrc();
      if (parameterSrc != null) {
         parameterSrc = CommonUtils.decodeURI(parameterSrc, "UTF-8", null);
         if (StringUtils.isNotBlank(parameterSrc)) {
            String[] paramArray = parameterSrc.split(",");
            if (paramArray.length > 0 && paramArray[0].startsWith("{{")) {
               result.add(new NameValueObject("SESSION_NAME", paramArray[0].substring(2)));
            }
            if (paramArray.length > 1) {
               result.add(new NameValueObject("SESSION_ID", paramArray[1]));
            }
            if (paramArray.length > 2) {
               result.add(new NameValueObject("SESSION_IP", paramArray[2]));
            }
            result.add(new NameValueObject("PARAM", parameterSrc));
            log.setListParameters(result);

            if (paramArray.length > 5) {
               String method = paramArray[paramArray.length - 4];
               if (method.startsWith("{")) {
                  method = method.substring(1);
               }
               if (method.endsWith("}")) {
                  method = method.substring(0, method.length() - 1);
               }
               String url = paramArray[paramArray.length - 5];
               if (url.startsWith("{")) {
                  url = url.substring(1);
               }
               if (url.endsWith("}")) {
                  url = url.substring(0, url.length() - 1);
               }
               log.setUrl(url + "." + method);
            }
         }
      }

//      List<SqlLog> sqlLogList = log.getSqlLogList();
//      if (CollectionUtils.isNotEmpty(sqlLogList)) {
//         SqlLog sqlLog = sqlLogList.get(0);
//         List<SqlResult> sqlResults = sqlLog.getResultList();
//         if (CollectionUtils.isNotEmpty(sqlResults)) {
//            SqlResult sqlResult = sqlResults.get(0);
//            if (sqlResult != null) {
//               List<RowResult> rowResults = sqlResult.getRowResults();
//               if (CollectionUtils.isNotEmpty(rowResults)) {
//                  RowResult rowResult = rowResults.get(0);
//                  if (rowResult != null) {
//                     String value = rowResult.getValue();
//                     String xml = getXml(value);
//                     String header = getHeader(value);
//                     rowResults.add(new RowResult(1, "HTTP", "0", xml));
//                     rowResults.add(new RowResult(2, "HEADER", "0", header));
//                  }
//               }
//            }
//         }
//      }
   }

   private String getXml(String value) {
      int idx = value.indexOf("<?xml ");
      if (idx == -1) {
         return "";
      }
      int idxLast = value.indexOf(DIFFGR, idx + 1);
      if (idxLast == -1) {
         idxLast = value.length();
      }
      return value.substring(idx, idxLast);
   }

   private String getHeader(String value) {
      int idx = value.indexOf("<?xml ");
      if (idx == -1) {
         return value;
      }
      String first = value.substring(0, idx);
      String last = "";
      int idxLast = value.indexOf(DIFFGR, idx + 1);
      if (idxLast != -1) {
         int vli = idxLast + DIFFGR.length();
         if (vli < value.length()) {
            last = value.substring(vli);
         }
      }
      return first + last;
   }

}
