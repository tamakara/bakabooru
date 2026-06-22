# Deployment

项目使用根目录 `docker-compose.yml` 编排。

## 服务

- `db`: PostgreSQL 16 + pgvector。
- `minio`: 图片对象存储。
- `redis`: 上传队列和设置缓存。
- `backend-ai-service`: FastAPI AI 推理服务。
- `backend-web-service`: Spring Boot 业务服务。
- `frontend`: Nginx + Vue 静态资源。

## 启动顺序

Web Service 依赖数据库、MinIO、Redis。AI Service 不再阻塞 Web/Frontend 启动；AI 未准备好时普通浏览和搜索仍可使用。

## 重要环境变量

数据库：

- `DB_HOST`
- `DB_PORT`
- `DB_USER`
- `DB_PASS`
- `DB_NAME`

MinIO：

- `MINIO_HOST`
- `MINIO_PORT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MINIO_BUCKET_NAME`

缩略图：

- `THUMBNAIL_MAX_SIZE`: 默认 `1024`。
- `THUMBNAIL_QUALITY`: 默认 `0.85`。
- `THUMBNAIL_FORMAT`: 默认 `jpg`。

AI：

- `AI_SERVICE_URL`
- `MODEL_CACHE_DIR`
- `AI_CONCURRENCY`: AI 后处理并发线程数，默认 `10`。

## 配置变更

修改缩略图尺寸后，新生成的缩略图会使用新路径。旧路径不会自动删除，后台 backfill 会按当前配置补齐缺失缩略图。
