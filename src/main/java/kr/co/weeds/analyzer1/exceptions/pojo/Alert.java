package kr.co.weeds.analyzer1.exceptions.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Alert {

  private String code;

  private String message;

  private String type;
}
