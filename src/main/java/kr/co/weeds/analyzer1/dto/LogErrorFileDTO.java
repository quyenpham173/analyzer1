package kr.co.weeds.analyzer1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LogErrorFileDTO {

   private String documentID;

   private String analysisTime;

   private String message;

}
