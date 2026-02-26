package kr.co.weeds.analyzer1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDTO {

   private int code;

   private String message;

   private Object data;

}
