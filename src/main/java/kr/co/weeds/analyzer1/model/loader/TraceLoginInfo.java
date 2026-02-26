package kr.co.weeds.analyzer1.model.loader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TraceLoginInfo {

   private int seq;
   private String systemId;
   private String traceId;
   private String loginUrl;
   private String userParamName;
   private String userParamFunc;

   public Map<String, Object> getUriInfo(String uriParam) {
      Map<String, Object> result = new HashMap<>();

      if (uriParam == null) {
         return Collections.emptyMap();
      }
      String uri;

      int idx = uriParam.indexOf('?');
      if (idx == -1) {
         result.put("uri", uriParam);
         result.put("params", new String[]{});
         return result;
      }

      uri = uriParam.substring(0, idx);

      String paramText = uriParam.substring(idx + 1);
      String[] params = paramText.split("&");
      result.put("uri", uri);
      result.put("params", params);

      return result;
   }

}
