package kr.co.weeds.analyzer1.repository.postgres;

import java.util.List;
import kr.co.weeds.analyzer1.entity.postgres.AnalyzeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyzeValueRepository extends JpaRepository<AnalyzeValue, Integer> {

   List<AnalyzeValue> findByAppId(String appId);

   void deleteByAppId(String appId);
}
