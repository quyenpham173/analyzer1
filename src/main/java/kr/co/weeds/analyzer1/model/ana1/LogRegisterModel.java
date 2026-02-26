package kr.co.weeds.analyzer1.model.ana1;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import kr.co.weeds.analyzer1.constant.StringConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogRegisterModel {

   @JsonProperty("logId")
   private String logId;

   @JsonProperty("logNum")
   private String logNum;

   @JsonProperty("startTime")
   @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = StringConstant.OS_DATE_FORMAT)
   private LocalDateTime startTime;

   @JsonProperty("createdTime")
   @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = StringConstant.OS_DATE_FORMAT)
   private LocalDateTime createdTime;

   @JsonProperty("systemInfo")
   private UserSystemInfo systemInfo;

   @JsonProperty("userInfo")
   private UserInfo userInfo;

}
