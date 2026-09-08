-- A downloaded model is only usable after the AI service initializes it on restart.
ALTER TABLE ai_models DROP CONSTRAINT IF EXISTS chk_ai_models_status;
ALTER TABLE ai_models ADD CONSTRAINT chk_ai_models_status
    CHECK (status IN ('NOT_INSTALLED', 'DOWNLOADING', 'RESTART_REQUIRED', 'READY', 'FAILED'));
