DELETE FROM system_settings
WHERE setting_key = 'upload.poll-interval';

INSERT INTO system_settings (setting_key, setting_value)
VALUES ('ai-job.max-attempts', '5'),
       ('ai-job.retry-base-delay-seconds', '30'),
       ('ai-job.retry-max-delay-seconds', '1800'),
       ('upload.completed-retention-days', '7')
ON CONFLICT (setting_key) DO NOTHING;
