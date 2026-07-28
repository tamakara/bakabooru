package com.tamakara.bakabooru.module.ai.repository;

import com.tamakara.bakabooru.module.ai.entity.AiJob;
import com.tamakara.bakabooru.module.ai.entity.AiJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AiJobRepository extends JpaRepository<AiJob, Long> {

    Optional<AiJob> findByImageId(Long imageId);

    @Query(value = """
            SELECT *
            FROM ai_jobs
            WHERE (status = 'PENDING' AND next_retry_at <= :now)
               OR (status = 'RUNNING' AND locked_until < :now)
            ORDER BY next_retry_at, created_at
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<AiJob> findNextClaimable(@Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE AiJob j
            SET j.lockedUntil = :lockedUntil, j.updatedAt = :now
            WHERE j.status = :status AND j.lockedBy = :workerId
            """)
    int extendWorkerLocks(
            @Param("workerId") String workerId,
            @Param("status") AiJobStatus status,
            @Param("lockedUntil") Instant lockedUntil,
            @Param("now") Instant now
    );
}
