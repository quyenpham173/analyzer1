package kr.co.weeds.analyzer1.repository.postgres;

import kr.co.weeds.analyzer1.entity.postgres.AnalyzeJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyzeJobRepository extends JpaRepository<AnalyzeJob, String> {

}
