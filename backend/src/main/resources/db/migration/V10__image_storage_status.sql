ALTER TABLE images
    ADD COLUMN storage_status TEXT NOT NULL DEFAULT 'AVAILABLE';

ALTER TABLE images
    ADD CONSTRAINT chk_images_storage_status
        CHECK (storage_status IN ('AVAILABLE', 'MISSING'));

CREATE INDEX idx_images_storage_status ON images (storage_status);
