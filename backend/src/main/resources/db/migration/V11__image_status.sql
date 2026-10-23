ALTER TABLE images ADD COLUMN image_status TEXT NOT NULL DEFAULT 'AVAILABLE';
ALTER TABLE images ADD CONSTRAINT chk_images_image_status
    CHECK (image_status IN ('AVAILABLE', 'PROCESSING', 'MISSING'));
CREATE INDEX idx_images_image_status ON images (image_status);
UPDATE images SET image_status = CASE WHEN storage_status = 'MISSING' THEN 'MISSING' ELSE 'AVAILABLE' END;
