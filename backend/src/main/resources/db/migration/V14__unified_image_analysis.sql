-- Unified image lifecycle and analysis metadata.
ALTER TABLE images ADD COLUMN IF NOT EXISTS image_status TEXT;
ALTER TABLE images ADD COLUMN IF NOT EXISTS analysis_stage TEXT;
ALTER TABLE images ADD COLUMN IF NOT EXISTS analysis_error TEXT;
ALTER TABLE images ADD COLUMN IF NOT EXISTS analysis_started_at TIMESTAMPTZ;
ALTER TABLE images ADD COLUMN IF NOT EXISTS analysis_completed_at TIMESTAMPTZ;

ALTER TABLE images DROP CONSTRAINT IF EXISTS chk_images_image_status;
-- V13 removed ai_status on new installations. Keep this migration compatible
-- with databases upgraded directly from older versions where it still exists.
DO $$
BEGIN
    -- Start with values that are available on every supported schema.
    UPDATE images
    SET image_status = CASE
        WHEN image_status IN ('MISSING', 'ANALYZING', 'ERROR', 'NORMAL') THEN image_status
        WHEN image_status = 'PROCESSING' THEN 'ANALYZING'
        WHEN image_status = 'AVAILABLE' THEN 'NORMAL'
        ELSE 'NORMAL'
    END;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'images' AND column_name = 'ai_status'
    ) THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'images' AND column_name = 'ai_error'
        ) THEN
            EXECUTE $sql$
                UPDATE images
                SET image_status = CASE
                    WHEN ai_status = 'PROCESSING' THEN 'ANALYZING'
                    WHEN ai_status = 'FAILED' OR ai_error IS NOT NULL THEN 'ERROR'
                    ELSE image_status
                END
            $sql$;
        ELSE
            EXECUTE $sql$
                UPDATE images
                SET image_status = CASE
                    WHEN ai_status = 'PROCESSING' THEN 'ANALYZING'
                    WHEN ai_status = 'FAILED' THEN 'ERROR'
                    ELSE image_status
                END
            $sql$;
        END IF;
    END IF;

    -- Some installations may have had ai_status removed while retaining
    -- ai_error. Handle that path without referencing a missing column.
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'images' AND column_name = 'ai_status'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'images' AND column_name = 'ai_error'
    ) THEN
        EXECUTE 'UPDATE images SET image_status = ''ERROR'' WHERE image_status <> ''MISSING'' AND ai_error IS NOT NULL';
    END IF;
END $$;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'images' AND column_name = 'ai_error') THEN
        EXECUTE 'UPDATE images SET analysis_error = ai_error WHERE analysis_error IS NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'images' AND column_name = 'ai_attempted_at') THEN
        EXECUTE 'UPDATE images SET analysis_started_at = ai_attempted_at WHERE analysis_started_at IS NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'images' AND column_name = 'ai_completed_at') THEN
        EXECUTE 'UPDATE images SET analysis_completed_at = ai_completed_at WHERE analysis_completed_at IS NULL';
    END IF;
END $$;

ALTER TABLE images ADD CONSTRAINT chk_images_image_status
    CHECK (image_status IN ('NORMAL', 'ANALYZING', 'ERROR', 'MISSING'));
CREATE INDEX IF NOT EXISTS idx_images_status ON images (image_status);
ALTER TABLE images DROP CONSTRAINT IF EXISTS chk_images_ai_status;
ALTER TABLE images DROP COLUMN IF EXISTS ai_status;

-- Preserve legacy vectors before removing the denormalized column.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'images' AND column_name = 'embedding') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'images' AND column_name = 'ai_completed_at') THEN
            EXECUTE $sql$
                INSERT INTO image_embeddings (image_id, model_id, model_revision, embedding, status, computed_at)
                SELECT id, 'clip-vit-base-patch32', '1', embedding, 'READY', ai_completed_at
                FROM images
                WHERE embedding IS NOT NULL
                ON CONFLICT DO NOTHING
            $sql$;
        ELSE
            EXECUTE $sql$
                INSERT INTO image_embeddings (image_id, model_id, model_revision, embedding, status, computed_at)
                SELECT id, 'clip-vit-base-patch32', '1', embedding, 'READY', NULL
                FROM images
                WHERE embedding IS NOT NULL
                ON CONFLICT DO NOTHING
            $sql$;
        END IF;
    END IF;
END $$;

-- Vectors are now stored only in image_embeddings.
ALTER TABLE image_embeddings ALTER COLUMN embedding DROP NOT NULL;
ALTER TABLE image_tag_relation DROP COLUMN IF EXISTS source_model_id;
ALTER TABLE images DROP COLUMN IF EXISTS tag_model_id;
ALTER TABLE images DROP COLUMN IF EXISTS embedding;
ALTER TABLE images DROP COLUMN IF EXISTS ai_error;
ALTER TABLE images DROP COLUMN IF EXISTS ai_attempted_at;
ALTER TABLE images DROP COLUMN IF EXISTS ai_completed_at;
ALTER TABLE images ALTER COLUMN image_status SET DEFAULT 'NORMAL';
ALTER TABLE images ALTER COLUMN image_status SET NOT NULL;
ALTER TABLE ai_models ADD COLUMN IF NOT EXISTS model_type TEXT;
ALTER TABLE ai_models DROP CONSTRAINT IF EXISTS chk_ai_models_status;
UPDATE ai_models SET status = 'NOT_INSTALLED' WHERE status IN ('AVAILABLE', 'DISABLED');
ALTER TABLE ai_models ADD CONSTRAINT chk_ai_models_status CHECK (status IN ('NOT_INSTALLED', 'DOWNLOADING', 'READY', 'FAILED'));
ALTER TABLE ai_jobs ADD COLUMN IF NOT EXISTS capability TEXT;
ALTER TABLE ai_jobs DROP CONSTRAINT IF EXISTS ai_jobs_image_id_key;
ALTER TABLE ai_jobs DROP CONSTRAINT IF EXISTS uk_ai_jobs_image_id;
UPDATE ai_jobs SET capability = CASE
    WHEN tag_model_id IS NOT NULL AND vector_model_ids IS NOT NULL THEN 'TAGS_AND_VECTORS'
    WHEN tag_model_id IS NOT NULL THEN 'TAGS'
    WHEN vector_model_ids IS NOT NULL THEN 'VECTORS'
    ELSE 'TAGS_AND_VECTORS'
END WHERE capability IS NULL;
ALTER TABLE ai_jobs ALTER COLUMN capability SET DEFAULT 'TAGS_AND_VECTORS';
ALTER TABLE ai_jobs ALTER COLUMN capability SET NOT NULL;

-- V10/V11 used PROCESSING and storage_status. Preserve active jobs and files
-- when upgrading those databases to the unified lifecycle.
UPDATE images i
SET image_status = 'ANALYZING',
    analysis_stage = CASE
        WHEN EXISTS (SELECT 1 FROM ai_jobs j WHERE j.image_id = i.id AND j.status IN ('PENDING', 'RUNNING') AND j.capability = 'TAGS')
         AND EXISTS (SELECT 1 FROM ai_jobs j WHERE j.image_id = i.id AND j.status IN ('PENDING', 'RUNNING') AND j.capability = 'VECTORS')
            THEN 'TAGS_AND_VECTORS'
        WHEN EXISTS (SELECT 1 FROM ai_jobs j WHERE j.image_id = i.id AND j.status IN ('PENDING', 'RUNNING') AND j.capability = 'VECTORS')
            THEN 'VECTORS'
        ELSE 'TAGS'
    END
FROM ai_jobs j
WHERE j.image_id = i.id AND j.status IN ('PENDING', 'RUNNING');

-- Keep historical completed jobs, but prevent duplicate active work for one
-- image/capability pair after the old one-job-per-image constraint is removed.
CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_jobs_active_image_capability
    ON ai_jobs (image_id, capability)
    WHERE status IN ('PENDING', 'RUNNING');
UPDATE ai_models SET model_type = CASE WHEN capability = 'TAGGING' THEN 'TAGGER' ELSE 'CLIP' END
WHERE model_type IS NULL;
ALTER TABLE ai_models ALTER COLUMN model_type SET DEFAULT 'CLIP';
ALTER TABLE ai_models DROP CONSTRAINT IF EXISTS chk_ai_models_model_type;
ALTER TABLE ai_models ADD CONSTRAINT chk_ai_models_model_type CHECK (model_type IN ('TAGGER', 'CLIP'));
DELETE FROM system_settings WHERE setting_key = 'ai.default-vector-models';
INSERT INTO system_settings (setting_key, setting_value)
VALUES ('ai.service-url', 'http://ai-service:8000'),
       ('ai.inference-concurrency', '1'),
       ('ai.device-mode', 'auto'),
       ('ai.model-cache-dir', '/model_cache')
ON CONFLICT (setting_key) DO NOTHING;
