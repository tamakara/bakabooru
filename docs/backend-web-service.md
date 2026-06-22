# Backend Web Service

Web Service 是业务核心，位于 `backend/web_service`，使用 Spring Boot 3、JPA、JdbcTemplate、Redis、MinIO 和 Flyway。

## 模块职责

- gallery: 上传任务、搜索入口、控制器。
- image: 图片实体、DTO、URL、搜索、缩略图、详情。
- tag: 标签字典、标签查询、图片标签关联。
- ai: AI Service 客户端、CLIP embedding 调用、AI 后处理调度。
- system: 系统设置与 Redis 缓存。

## 上传流程

上传任务只负责业务入库：

1. 保存临时文件。
2. 计算 SHA-256 hash 并查重。
3. 解析图片尺寸和格式。
4. 上传 `original/{hash}`。
5. 按 `app.thumbnail` 生成并上传 `thumbnail/{maxSize}/{hash}.{format}`。
6. 图片以 `PENDING` 状态入库。
7. 调用 `AiProcessingService` 异步开始 AI 后处理。

## AI 后处理

AI 后处理通过独立线程池 `aiExecutor` 执行，与上传线程分离。并发数由 `AI_CONCURRENCY` 环境变量控制，默认 `10`。

状态流转：

- `PENDING`: 已入库但未完成 AI 计算，或上次计算失败。
- `PROCESSING`: 正在打标和计算 CLIP。
- `READY`: 标签和 embedding 已写入。

失败策略：

- 失败后不自动重试。
- 状态回到 `PENDING`。
- 错误写入 `aiError`。
- 详情页可手动触发重试。

## 搜索

搜索由 native SQL/JdbcTemplate 执行：

- 使用 `LIMIT size + 1` 判断 `hasNext`。
- 不返回精确总数。
- 支持标签、关键字、尺寸、文件大小、AI 状态过滤。
- 语义搜索使用 CLIP 文本 embedding 和 pgvector 距离排序。
- 搜索结果只拼 URL，不访问 MinIO 检查文件，也不生成缩略图。
