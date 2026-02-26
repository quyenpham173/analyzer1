package kr.co.weeds.analyzer1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import kr.co.weeds.analyzer1.model.loader.OpUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpUserResponseDTO {

   @JsonProperty("result")
   private String result;

   @JsonProperty("result_message")
   private String resultMessage;

   @JsonProperty("result_info_list")
   private List<OpUser> opUserList;

   @JsonProperty("result_info")
   private OpUser opUser;

}
