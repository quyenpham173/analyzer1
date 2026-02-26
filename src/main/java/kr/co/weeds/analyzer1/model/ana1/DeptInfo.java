package kr.co.weeds.analyzer1.model.ana1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeptInfo {

   @JsonProperty("code")
   private String code;

   @JsonProperty("kind")
   private String kind;

   @JsonProperty("name")
   private String name;

}
