ALTER TABLE images DROP CONSTRAINT IF EXISTS chk_images_ai_status;
DROP INDEX IF EXISTS idx_images_ai_status;
ALTER TABLE images DROP COLUMN IF EXISTS ai_status;
