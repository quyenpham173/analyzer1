package kr.co.weeds.analyzer1.model.loader;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AppInfo {

   protected int appSeq;
   protected String appName;
   protected String sysId;
   protected String url;
   protected String params;
   protected String method;
   protected String perfType;
   protected Boolean infoUsageYn;
   protected Boolean infoDetYn;
   protected String appAttributeCode;
   protected Boolean viewYn;

}
