package kr.co.weeds.analyzer1.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JobType {

   FIXED_DELAY("FIXED_DELAY"), FIXED_RATE("FIXED_RATE"), CRON("CRON");

   private final String value;

   JobType(String value) {
      this.value = value;
   }

   @Override
   @JsonValue
   public String toString() {
      return String.valueOf(value);
   }

   @JsonCreator
   public static JobType fromValue(String text) {
      for (JobType type : JobType.values()) {
         if (String.valueOf(type.value).equals(text)) {
            return type;
         }
      }
      return null;
   }

}
