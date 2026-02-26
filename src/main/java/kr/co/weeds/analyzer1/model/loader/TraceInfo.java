package kr.co.weeds.analyzer1.model.loader;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TraceInfo {

   private String parentSysId;
   private String traceId;

   private String traceName;
   private String traceType;
   private String tracePath;
   private String traceIp;
   private String systemId;
   private String systemType;
   private String systemName;
   private String userParamFunc;
   private String titleFunc;
   private String logType;
   private Map<String, String> privateInfoFunc;

   public static TraceInfo newTraceInfoFromParent(String traceId, TraceInfo parentTraceInfo) {
      TraceInfo result = new TraceInfo();
      result.setTraceId(traceId);
      result.setTraceName(traceId);
      result.setTraceType(parentTraceInfo.getTraceType());
      result.setTracePath(parentTraceInfo.getTracePath());
      result.setTraceIp(parentTraceInfo.getTraceIp());
      result.setSystemId(traceId);
      result.setSystemType(parentTraceInfo.getSystemType());
      result.setUserParamFunc(parentTraceInfo.getUserParamFunc());
      result.setTitleFunc(parentTraceInfo.getTitleFunc());
      result.setLogType(parentTraceInfo.getLogType());
      result.setPrivateInfoFunc(parentTraceInfo.getPrivateInfoFunc());
      return result;
   }

}
