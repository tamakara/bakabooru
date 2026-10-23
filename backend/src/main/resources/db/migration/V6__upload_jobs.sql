CREATE TABLE upload_jobs
(
    id                  UUID PRIMARY KEY,
    filename            TEXT        NOT NULL,
    staging_object_name TEXT        NOT NULL UNIQUE,
    size                BIGINT      NOT NULL,
    status              VARCHAR(20) NOT NULL,
    attempts            INTEGER     NOT NULL DEFAULT 0,
    error_message       TEXT,
    locked_by           TEXT,
    locked_until        TIMESTAMPTZ,
    image_id            BIGINT REFERENCES images (id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    completed_at        TIMESTAMPTZ,
    CONSTRAINT chk_upload_jobs_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_upload_jobs_claimable
    ON upload_jobs (created_at)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX idx_upload_jobs_status_updated
    ON upload_jobs (status, updated_at DESC);
