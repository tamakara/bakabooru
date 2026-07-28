UPDATE images
SET ai_status = 'PENDING',
    ai_error = NULL,
    ai_completed_at = NULL
WHERE ai_status = 'PROCESSING';

ALTER TABLE images
    DROP CONSTRAINT IF EXISTS chk_images_ai_status;

ALTER TABLE images
    ADD CONSTRAINT chk_images_ai_status
        CHECK (ai_status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED'));

CREATE TABLE ai_jobs
(
    id              BIGSERIAL PRIMARY KEY,
    image_id        BIGINT      NOT NULL UNIQUE REFERENCES images (id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL,
    attempts        INTEGER     NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMPTZ NOT NULL,
    locked_by       TEXT,
    locked_until    TIMESTAMPTZ,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    completed_at    TIMESTAMPTZ,
    CONSTRAINT chk_ai_jobs_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_ai_jobs_pending
    ON ai_jobs (next_retry_at, created_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_ai_jobs_expired_locks
    ON ai_jobs (locked_until)
    WHERE status = 'RUNNING';

INSERT INTO ai_jobs (image_id, status, attempts, next_retry_at, error_message, created_at, updated_at)
SELECT id,
       'PENDING',
       0,
       CURRENT_TIMESTAMP,
       ai_error,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM images
WHERE ai_status <> 'READY'
ON CONFLICT (image_id) DO NOTHING;

DROP INDEX IF EXISTS idx_tags_embedding;
ALTER TABLE tags DROP COLUMN IF EXISTS embedding;
