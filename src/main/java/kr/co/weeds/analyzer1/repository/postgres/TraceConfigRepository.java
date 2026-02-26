package kr.co.weeds.analyzer1.repository.postgres;

import java.util.List;
import java.util.Optional;
import kr.co.weeds.analyzer1.entity.postgres.TraceAnalyzeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TraceConfigRepository extends JpaRepository<TraceAnalyzeConfig, Integer> {

   List<TraceAnalyzeConfig> findByTraceIdInAndAppId(List<String> traceIds, String appId);

   Optional<TraceAnalyzeConfig> findByTraceIdAndAppId(String traceId, String appId);
}
