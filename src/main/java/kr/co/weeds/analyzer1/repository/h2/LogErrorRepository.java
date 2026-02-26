package kr.co.weeds.analyzer1.repository.h2;

import java.util.List;
import java.util.Set;
import kr.co.weeds.analyzer1.entity.h2.LogError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LogErrorRepository extends JpaRepository<LogError, String> {

   List<LogError> findByDocumentIdIn(Set<String> documentIds);

   @Query(value = "SELECT * FROM log_error ORDER BY retry_count DESC LIMIT :limit", nativeQuery = true)
   List<LogError> findWithLimit(@Param("limit") int limit);

}
