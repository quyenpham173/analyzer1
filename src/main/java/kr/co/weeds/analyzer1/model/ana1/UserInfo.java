package kr.co.weeds.analyzer1.model.ana1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {

   @JsonProperty("userId")
   private String userId;

   @JsonProperty("userCode")
   private Integer userCode;

   @JsonProperty("userName")
   private String userName;

   @JsonProperty("clientIp")
   private String clientIp;

   @JsonProperty("position")
   private String position;

   @JsonProperty("duty")
   private String duty;

   @JsonProperty("deptInfo")
   private List<DeptInfo> deptInfo;

   public String getUserId() {
      return userId == null ? StringUtils.EMPTY : userId;
   }

   public String getUserName() {
      return userName == null ? StringUtils.EMPTY : userName;
   }

   public Integer getUserCode() {
      return userCode == null ? 0 : userCode;
   }

   public String getClientIp() {
      return clientIp == null ? StringUtils.EMPTY : clientIp;
   }

   public String getPosition() {
      return position == null ? StringUtils.EMPTY : position;
   }

   public String getDuty() {
      return duty == null ? StringUtils.EMPTY : duty;
   }

   public List<DeptInfo> getDeptInfo() {
      return deptInfo == null ? Collections.emptyList() : deptInfo;
   }
}
