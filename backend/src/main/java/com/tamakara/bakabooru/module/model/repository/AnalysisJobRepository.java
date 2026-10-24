package com.tamakara.bakabooru.module.model.repository;

import com.tamakara.bakabooru.module.model.entity.AnalysisJob;
import com.tamakara.bakabooru.module.model.entity.AnalysisJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {

    long countByStatus(AnalysisJobStatus status);

    List<AnalysisJob> findAllByImageId(Long imageId);

    Optional<AnalysisJob> findFirstByImageIdAndCapabilityOrderByUpdatedAtDesc(Long imageId, String capability);

    Optional<AnalysisJob> findFirstByImageIdOrderByCreatedAtDesc(Long imageId);

    Optional<AnalysisJob> findFirstByImageIdOrderByUpdatedAtDesc(Long imageId);

    boolean existsByImageIdAndStatusIn(Long imageId, List<AnalysisJobStatus> statuses);

    @Query(value = """
            SELECT *
            FROM ai_jobs
            WHERE (status = 'PENDING' AND next_retry_at <= :now)
               OR (status = 'RUNNING' AND locked_until < :now)
            ORDER BY next_retry_at, created_at
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<AnalysisJob> findNextClaimable(@Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE AnalysisJob j
            SET j.lockedUntil = :lockedUntil, j.updatedAt = :now
            WHERE j.status = :status AND j.lockedBy = :workerId
            """)
    int extendWorkerLocks(
            @Param("workerId") String workerId,
            @Param("status") AnalysisJobStatus status,
            @Param("lockedUntil") Instant lockedUntil,
            @Param("now") Instant now
    );
}
