package com.tamakara.bakabooru.module.upload.repository;

import com.tamakara.bakabooru.module.upload.entity.UploadJob;
import com.tamakara.bakabooru.module.upload.entity.UploadJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadJobRepository extends JpaRepository<UploadJob, UUID> {

    @Query(value = """
            SELECT *
            FROM upload_jobs
            WHERE status = 'PENDING'
               OR (status = 'PROCESSING' AND locked_until < :now)
            ORDER BY created_at
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<UploadJob> findNextClaimable(@Param("now") Instant now);

    long countByStatus(UploadJobStatus status);

    Optional<UploadJob> findFirstByStatusOrderByCreatedAtAsc(UploadJobStatus status);

    List<UploadJob> findTop100ByStatusOrderByUpdatedAtDesc(UploadJobStatus status);

    List<UploadJob> findByStatusOrderByUpdatedAtDesc(UploadJobStatus status);

    List<UploadJob> findByStatusAndCompletedAtBefore(UploadJobStatus status, Instant completedBefore);

    @Modifying
    @Query("""
            UPDATE UploadJob j
            SET j.lockedUntil = :lockedUntil, j.updatedAt = :now
            WHERE j.status = :status AND j.lockedBy = :workerId
            """)
    int extendWorkerLocks(
            @Param("workerId") String workerId,
            @Param("status") UploadJobStatus status,
            @Param("lockedUntil") Instant lockedUntil,
            @Param("now") Instant now
    );
}
