package kr.co.weeds.analyzer1.exceptions.pojo;

import kr.co.weeds.analyzer1.exceptions.constants.AlertType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlertCode {

  private String code;
  private String label;
  private AlertType type;
}
