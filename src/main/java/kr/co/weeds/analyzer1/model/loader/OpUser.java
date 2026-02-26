package kr.co.weeds.analyzer1.model.loader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import kr.co.weeds.analyzer1.model.ana1.DeptInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpUser {

   @JsonProperty("opuser_code")
   private Integer opUserCode;

   @JsonProperty("opuser_name")
   private String opUserName;

   @JsonProperty("opuser_id")
   private String opUserId;

   @JsonProperty("ip")
   private String ip;

   @JsonProperty("position")
   private String position;

   @JsonProperty("duty")
   private String duty;

   @JsonProperty("email")
   private String email;

   @JsonProperty("tel")
   private String tel;

   @JsonProperty("mobile")
   private String mobile;

   @JsonProperty("retire_yn")
   private String retireYn;

   @JsonProperty("deptInfo")
   private List<DeptInfo> deptInfo;

   @JsonProperty("u_status_code")
   private String statusCode;

}
