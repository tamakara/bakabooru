-- Update only the previous default; keep any user-configured endpoint intact.
UPDATE system_settings
SET setting_value = 'http://model-manager:8000'
WHERE setting_key = 'ai.service-url'
  AND setting_value = 'http://ai-service:8000';
