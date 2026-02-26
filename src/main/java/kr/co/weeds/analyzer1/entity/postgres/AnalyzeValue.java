package kr.co.weeds.analyzer1.entity.postgres;

import java.util.Date;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "analyze_query_value", schema = "analy")
public class AnalyzeValue {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Integer id;

   @Column(name = "doc_id")
   private String docId;

   @Column(name = "app_id")
   private String appId;

   @Column(name = "created_time")
   private String createdTime;

   @Column(name = "analysis_time")
   private Date analysisTime;

}
