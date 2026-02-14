-- 启用向量扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建数据库表

--- 图片表
CREATE TABLE images
(
    id         BIGSERIAL PRIMARY KEY,
    file_name  TEXT      NOT NULL,
    extension  TEXT      NOT NULL,
    size       BIGINT    NOT NULL,
    width      INTEGER   NOT NULL,
    height     INTEGER   NOT NULL,
    title      TEXT      NOT NULL,
    hash       CHAR(64)  NOT NULL,
    view_count BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    tags       JSONB     NOT NULL,
    embedding  vector(512)
);
CREATE INDEX idx_images_embedding ON images USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_images_tags ON images USING GIN (tags);
CREATE UNIQUE INDEX idx_images_hash ON images (hash);

--- 标签字典表
CREATE TABLE tags
(
    id        SERIAL PRIMARY KEY,
    name      TEXT UNIQUE NOT NULL,
    type      TEXT        NOT NULL,
    embedding vector(384)
);
CREATE INDEX idx_tags_name ON tags (name);
CREATE INDEX idx_tags_embedding ON tags USING hnsw (embedding vector_cosine_ops);

--- 系统设置表
CREATE TABLE system_settings
(
    setting_key   TEXT PRIMARY KEY,
    setting_value TEXT
);

