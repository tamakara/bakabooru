INSERT IGNORE INTO system_settings (setting_key, setting_value)
VALUES ('upload.allowed-extensions', 'jpg,png,webp,gif,jpeg'),
       ('upload.concurrency', '3'),
       ('upload.poll-interval', '1000'),
       ('file.thumbnail.quality', '80'),
       ('file.thumbnail.max-size', '800'),
       ('tag.threshold', '0.6'),
       ('tag.initialized', 'false'),
       ('auth.password', ''),
       ('auth.initialized', 'false'),
       ('llm.url', ''),
       ('llm.model', ''),
       ('llm.api-key', '');