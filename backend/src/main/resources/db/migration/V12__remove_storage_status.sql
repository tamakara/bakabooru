ALTER TABLE images DROP CONSTRAINT IF EXISTS chk_images_storage_status;
DROP INDEX IF EXISTS idx_images_storage_status;
ALTER TABLE images DROP COLUMN IF EXISTS storage_status;
