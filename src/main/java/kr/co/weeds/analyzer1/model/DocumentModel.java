package kr.co.weeds.analyzer1.model;

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
public class DocumentModel<T> {

   @JsonProperty("_index")
   private String index;

   @JsonProperty("_id")
   private String docId;

   @JsonProperty("_source")
   private T source;

}
