-- Unified runtime setting names. PostgreSQL remains the source of truth.
INSERT INTO system_settings (setting_key, setting_value)
SELECT 'analysis.tag-threshold', setting_value FROM system_settings WHERE setting_key = 'tag.threshold'
ON CONFLICT (setting_key) DO NOTHING;
INSERT INTO system_settings (setting_key, setting_value)
SELECT 'analysis.max-attempts', setting_value FROM system_settings WHERE setting_key = 'ai-job.max-attempts'
ON CONFLICT (setting_key) DO NOTHING;
INSERT INTO system_settings (setting_key, setting_value)
SELECT 'analysis.retry-base-delay-seconds', setting_value FROM system_settings WHERE setting_key = 'ai-job.retry-base-delay-seconds'
ON CONFLICT (setting_key) DO NOTHING;
INSERT INTO system_settings (setting_key, setting_value)
SELECT 'analysis.retry-max-delay-seconds', setting_value FROM system_settings WHERE setting_key = 'ai-job.retry-max-delay-seconds'
ON CONFLICT (setting_key) DO NOTHING;
INSERT INTO system_settings (setting_key, setting_value)
SELECT 'upload.completed-retention-days', setting_value FROM system_settings WHERE setting_key = 'upload.completed-retention-days'
ON CONFLICT (setting_key) DO NOTHING;
INSERT INTO system_settings (setting_key, setting_value)
SELECT 'inference.concurrency', setting_value FROM system_settings WHERE setting_key = 'ai.inference-concurrency'
ON CONFLICT (setting_key) DO NOTHING;
INSERT INTO system_settings (setting_key, setting_value)
VALUES ('model.active-tagger-id', NULL),
       ('model.active-clip-id', NULL)
ON CONFLICT (setting_key) DO NOTHING;
DELETE FROM system_settings WHERE setting_key IN (
    'tag.threshold', 'ai-job.max-attempts', 'ai-job.retry-base-delay-seconds',
    'ai-job.retry-max-delay-seconds', 'ai.inference-concurrency',
    'ai.service-url', 'ai.device-mode', 'ai.model-cache-dir', 'ai.default-vector-models'
);

ALTER TABLE ai_models DROP CONSTRAINT IF EXISTS chk_ai_models_status;
ALTER TABLE ai_models ADD CONSTRAINT chk_ai_models_status
    CHECK (status IN ('NOT_INSTALLED', 'DOWNLOADING', 'RESTART_REQUIRED', 'READY', 'FAILED'));
