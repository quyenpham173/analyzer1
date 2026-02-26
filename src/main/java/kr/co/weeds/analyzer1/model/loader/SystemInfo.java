package kr.co.weeds.analyzer1.model.loader;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemInfo {

   private String systemId;

   private String systemName;

   private String systemType;

   private int systemDepth;

   private int systemPosition;

   private String systemIdUp;

}
