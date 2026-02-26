package kr.co.weeds.analyzer1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnalyzeJobDTO {

   private String jobKey;

   private String jobType;

   private String jobValue;

   private String jobMethod;

   private String description;

   private Boolean enabled;

}
