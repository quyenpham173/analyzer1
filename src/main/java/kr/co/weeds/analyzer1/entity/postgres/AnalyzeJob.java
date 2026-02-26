package kr.co.weeds.analyzer1.entity.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "analyze_schedule", schema = "analy")
public class AnalyzeJob {

   @Id
   @Column(name = "job_key")
   private String jobKey;

   @Column(name = "job_type")
   private String jobType;

   @Column(name = "job_value")
   private String jobValue;

   @Column(name = "job_method")
   private String jobMethod;

   @Column(name = "description")
   private String description;

   @Column(name = "enabled")
   private Boolean enabled;

}
