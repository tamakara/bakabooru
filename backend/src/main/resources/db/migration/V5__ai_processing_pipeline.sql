ALTER TABLE images
    ADD COLUMN IF NOT EXISTS ai_error TEXT,
    ADD COLUMN IF NOT EXISTS ai_attempted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ai_completed_at TIMESTAMP;

UPDATE images
SET ai_status = 'PENDING'
WHERE ai_status = 'FAILED';

CREATE INDEX IF NOT EXISTS idx_images_ai_status
    ON images (ai_status);
