-- 启用向量扩展
CREATE EXTENSION IF NOT EXISTS vector;

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
    hash       CHAR(64)  NOT NULL UNIQUE,
    view_count BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    tags       JSONB     NOT NULL,
    embedding  vector(512)
);
CREATE INDEX idx_images_tags ON images USING GIN (tags);

--- 标签字典表
CREATE TABLE tags
(
    id        BIGSERIAL PRIMARY KEY,
    name      TEXT UNIQUE NOT NULL,
    type      TEXT        NOT NULL,
    embedding vector(384)
);
CREATE INDEX idx_tags_name ON tags (name);
CREATE INDEX idx_tags_embedding ON tags USING hnsw (embedding vector_cosine_ops);

CREATE TABLE image_tag_relation
(
    id       BIGSERIAL PRIMARY KEY,
    image_id BIGINT           NOT NULL REFERENCES images (id),
    tag_id   INTEGER          NOT NULL REFERENCES tags (id),
    score    DOUBLE PRECISION NOT NULL,
    UNIQUE (image_id, tag_id)
);