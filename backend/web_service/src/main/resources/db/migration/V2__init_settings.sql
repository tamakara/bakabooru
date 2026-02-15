--- 系统设置表
CREATE TABLE system_settings
(
    setting_key   TEXT PRIMARY KEY,
    setting_value TEXT
);


INSERT INTO system_settings (setting_key, setting_value)
VALUES ('system.tag-initialized', 'false'),
       ('system.auth-initialized', 'false'),
       ('system.auth-password', ''),

       ('upload.poll-interval', '1000'),
       ('file.thumbnail.size', '800'),
       ('tag.threshold', '0.61'),
       ('llm.url', 'https://dashscope.aliyuncs.com/compatible-mode/v1'),
       ('llm.model', 'deepseek-v3.2'),
       ('llm.api-key', '');