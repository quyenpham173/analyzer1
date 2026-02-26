package kr.co.weeds.analyzer1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TraceConfigDTO {

   private String traceId;

   private String systemId;

   private String configuration;

   private String appId;

}
