package kr.co.weeds.analyzer1.entity.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "tn_trace_config", schema = "analy")
public class TraceAnalyzeConfig {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Integer id;

   @Column(name = "trace_id")
   private String traceId;

   @Column(name = "system_id")
   private String systemId;

   @Column(name = "configuration")
   private String configuration;

   @Column(name = "app_id")
   private String appId;

}
