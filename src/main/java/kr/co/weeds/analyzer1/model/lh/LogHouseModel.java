package kr.co.weeds.analyzer1.model.lh;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import kr.co.weeds.analyzer1.constant.StringConstant;
import kr.co.weeds.dtos.log.sql.SqlLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import kr.co.weeds.dtos.NameValueObject;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogHouseModel {

   @JsonProperty("logId")
   private String logId;

   @JsonProperty("logNum")
   private String logNum;

   @JsonProperty("startTime")
   @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = StringConstant.OS_DATE_FORMAT)
   private LocalDateTime startTime;

   @JsonProperty("createdTime")
   @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = StringConstant.OS_DATE_FORMAT)
   private LocalDateTime createdTime;

   @JsonProperty("clientIp")
   private String clientIp;

   @JsonProperty("url")
   private String url;

   @JsonProperty("traceId")
   private String traceId;

   @JsonProperty("parentTraceId")
   private String parentTraceId;

   @JsonProperty("additionalData1")
   private String additionalData1;

   @JsonProperty("additionalData2")
   private String additionalData2;

   @JsonProperty("code")
   private String code;

   @JsonProperty("listHeaders1")
   protected List<NameValueObject> listHeaders1;

   @JsonProperty("listHeaders2")
   protected List<NameValueObject> listHeaders2;

   @JsonProperty("listCookies1")
   private List<NameValueObject> listCookies1;

   @JsonProperty("listCookies2")
   private List<NameValueObject> listCookies2;

   @JsonProperty("listSessions1")
   private List<NameValueObject> listSessions1;

   @JsonProperty("listSessions2")
   private List<NameValueObject> listSessions2;

   @JsonProperty("listParameters")
   private List<NameValueObject> listParameters;

   @JsonProperty("sqlLogList")
   private List<SqlLog> sqlLogList;

   public String getParameterSrc() {
      if (CollectionUtils.isNotEmpty(listParameters)) {
         return listParameters.stream()
             .map(param -> param.getName() + "=" + URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8)).collect(
                 Collectors.joining("&"));
      }
      return StringUtils.EMPTY;
   }

}
