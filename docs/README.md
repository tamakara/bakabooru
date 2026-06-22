# BaKaBooru Docs

BaKaBooru 是本地优先的图库管理系统，核心由 Spring Boot Web 服务、FastAPI AI 服务、Vue 前端、PostgreSQL/pgvector、MinIO 和 Redis 组成。

## 文档索引

- [architecture.md](architecture.md): 系统架构、服务边界、主要数据流。
- [backend-web-service.md](backend-web-service.md): Web 服务模块、上传、搜索、AI 后处理。
- [ai-service.md](ai-service.md): AI 服务、CLIP、Tagger、模型加载策略。
- [frontend.md](frontend.md): 前端页面、状态筛选、详情页重试。
- [deployment.md](deployment.md): Docker Compose、环境变量、启动顺序。
- [operations.md](operations.md): 缩略图 backfill、AI 状态、故障排查。
- [data-model.md](data-model.md): 核心表、向量字段、索引和状态字段。
