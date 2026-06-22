# Architecture

BaKaBooru 采用本地优先架构。Web 服务负责业务 API、数据库事务、对象存储 URL 和任务调度；AI 服务只负责模型推理；前端只通过 Web 服务访问业务能力。

## 服务边界

- Frontend: Vue 3 + Naive UI，提供上传、搜索、详情、设置页面。
- Web Service: Spring Boot，负责鉴权、图片元数据、搜索、上传任务、AI 后处理调度。
- AI Service: FastAPI，负责 Camie Tagger、CLIP 文本向量、CLIP 图像向量。
- PostgreSQL + pgvector: 保存图片元数据、标签关系、向量。
- MinIO: 保存原图和缩略图。
- Redis: 上传任务队列和系统设置缓存。

## 关键数据流

上传图片：

1. 前端上传文件到 Web Service。
2. Web Service 计算 hash、查重、解析尺寸。
3. Web Service 上传 original，并生成当前配置尺寸的 thumbnail。
4. 图片以 `PENDING` 状态入库。
5. AI 后处理异步开始，完成后更新为 `READY`。

搜索图片：

1. 普通搜索只访问 PostgreSQL，返回轻量 DTO 和确定性对象 URL。
2. 语义搜索先请求 AI Service 生成 CLIP 文本向量，再由 pgvector 排序。
3. 搜索结果使用 `hasNext`，不执行复杂总数 count。

AI 后处理：

1. Web Service 将图片状态改为 `PROCESSING`。
2. AI Service 对 original 生成标签和图像 CLIP embedding。
3. 成功后 Web Service 写入标签、embedding 和 `READY`。
4. 失败后状态回到 `PENDING`，写入 `aiError`，等待手动重试。
