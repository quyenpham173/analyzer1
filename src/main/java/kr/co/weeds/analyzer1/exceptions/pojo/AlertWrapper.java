package kr.co.weeds.analyzer1.exceptions.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertWrapper {

  @JsonProperty("alerts")
  private Set<Alert> alerts;
}
