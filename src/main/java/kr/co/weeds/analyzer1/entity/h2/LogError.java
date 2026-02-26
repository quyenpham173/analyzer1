package kr.co.weeds.analyzer1.entity.h2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "log_error")
public class LogError {

   @Id
   @Column(name = "document_id")
   private String documentId;

   @Column(name = "retry_count")
   private Integer retryCount;

   @Column(name = "last_run")
   private Date lastRun;

   @Column(name = "message")
   private String message;

   public void setMessage(String message) {
      if (message != null && message.length() > 255) {
         this.message = message.substring(0, 255);
      } else {
         this.message = message;
      }
   }

}
