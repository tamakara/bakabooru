CREATE TABLE ai_models
(
    id                 TEXT PRIMARY KEY,
    name               TEXT NOT NULL,
    capability         TEXT NOT NULL,
    version            TEXT NOT NULL,
    dimension          INTEGER,
    status             TEXT NOT NULL DEFAULT 'AVAILABLE',
    artifact_object    TEXT,
    download_url       TEXT,
    artifact_sha256    TEXT,
    preprocessing_json TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_ai_models_capability CHECK (capability IN ('TEXT_TO_IMAGE', 'IMAGE_TO_IMAGE', 'TAGGING')),
    CONSTRAINT chk_ai_models_status CHECK (status IN ('AVAILABLE', 'DOWNLOADING', 'READY', 'DISABLED', 'FAILED')),
    UNIQUE (id, version)
);

INSERT INTO ai_models (id, name, capability, version, dimension, status, preprocessing_json)
VALUES ('clip-vit-base-patch32', 'CLIP ViT-B/32', 'TEXT_TO_IMAGE', '1', 512, 'AVAILABLE',
        '{"adapter":"clip","size":224,"color":"RGB","layout":"NCHW"}'),
       ('camie-tagger-v2', 'Camie Tagger V2', 'TAGGING', '1', NULL, 'AVAILABLE',
        '{"adapter":"camie-tagger-v2"}')
ON CONFLICT (id) DO NOTHING;

CREATE TABLE image_embeddings
(
    image_id       BIGINT      NOT NULL REFERENCES images (id) ON DELETE CASCADE,
    model_id       TEXT        NOT NULL REFERENCES ai_models (id),
    model_revision TEXT        NOT NULL,
    embedding      vector      NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'READY',
    error_message  TEXT,
    computed_at    TIMESTAMPTZ,
    PRIMARY KEY (image_id, model_id, model_revision),
    CONSTRAINT chk_image_embeddings_status CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED'))
);

CREATE INDEX idx_image_embeddings_model ON image_embeddings (model_id, status, image_id);

INSERT INTO image_embeddings (image_id, model_id, model_revision, embedding, status, computed_at)
SELECT id, 'clip-vit-base-patch32', '1', embedding, 'READY', ai_completed_at
FROM images
WHERE embedding IS NOT NULL
ON CONFLICT DO NOTHING;

ALTER TABLE images ADD COLUMN IF NOT EXISTS tag_model_id TEXT REFERENCES ai_models (id);

ALTER TABLE image_tag_relation
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN IF NOT EXISTS source_model_id TEXT REFERENCES ai_models (id);

ALTER TABLE upload_jobs
    ADD COLUMN IF NOT EXISTS tag_model_id TEXT,
    ADD COLUMN IF NOT EXISTS vector_model_ids TEXT;

ALTER TABLE ai_jobs
    ADD COLUMN IF NOT EXISTS tag_model_id TEXT,
    ADD COLUMN IF NOT EXISTS vector_model_ids TEXT;

INSERT INTO system_settings (setting_key, setting_value)
VALUES ('ai.default-vector-models', 'clip-vit-base-patch32')
ON CONFLICT (setting_key) DO NOTHING;
