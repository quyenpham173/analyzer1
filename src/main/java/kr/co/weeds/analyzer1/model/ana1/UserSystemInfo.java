package kr.co.weeds.analyzer1.model.ana1;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSystemInfo {

   @JsonProperty("systemId")
   private String systemId;

   @JsonProperty("systemName")
   private String systemName;

   @JsonProperty("systemType")
   private String systemType;

   @JsonProperty("traceId")
   private String traceId;

   @JsonProperty("traceName")
   private String traceName;

   @JsonProperty("traceType")
   private String traceType;

   @JsonProperty("parentSysId")
   private String parentSysId;

   @JsonProperty("logType")
   private String logType;

   public String getSystemId() {
      return systemId == null ? StringUtils.EMPTY : systemId;
   }

   public String getSystemName() {
      return systemName == null ? StringUtils.EMPTY : systemName;
   }

   public String getSystemType() {
      return systemType == null ? StringUtils.EMPTY : systemType;
   }

   public String getTraceId() {
      return traceId == null ? StringUtils.EMPTY : traceId;
   }

   public String getParentSysId() {
      return parentSysId == null ? StringUtils.EMPTY : parentSysId;
   }

   public String getLogType() {
      return logType == null ? StringUtils.EMPTY : logType;
   }
}
