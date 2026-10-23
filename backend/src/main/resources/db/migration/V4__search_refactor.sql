DELETE FROM system_settings
WHERE setting_key IN ('file.thumbnail.size', 'llm.url', 'llm.model', 'llm.api-key');

ALTER TABLE images
    ADD COLUMN IF NOT EXISTS ai_status TEXT NOT NULL DEFAULT 'PENDING';

UPDATE images
SET ai_status = CASE WHEN embedding IS NULL THEN 'PENDING' ELSE 'READY' END;

CREATE INDEX IF NOT EXISTS idx_image_tag_relation_tag_image
    ON image_tag_relation (tag_id, image_id);

CREATE INDEX IF NOT EXISTS idx_images_created_at
    ON images (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_images_size
    ON images (size);

CREATE INDEX IF NOT EXISTS idx_images_dimensions
    ON images (width, height);
