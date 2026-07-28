# Web Service

Web Service 位于根目录的 `web-service`，是系统的业务核心和对外 API 边界。它使用 Java 21、Spring Boot 3.5、JPA/JdbcTemplate、Flyway 与 MinIO，并通过内部 HTTP 调用 AI Service。

## 模块结构

```mermaid
flowchart TB
    Controller["HTTP Controllers"] --> Gallery["gallery.service<br/>搜索编排"]
    Controller --> Upload["upload.service<br/>持久化上传任务"]
    Controller --> Image["image.service<br/>图片、URL、缩略图"]
    Controller --> Tag["tag.service<br/>标签与关系"]
    Controller --> System["system.service<br/>鉴权与设置"]
    Gallery --> Image
    Gallery --> AI["ai.service<br/>推理客户端与后处理"]
    AI --> Tag
    AI --> Image

    Image --> PG[("PostgreSQL")]
    Tag --> PG
    System --> PG
    Image --> MinIO[("MinIO")]
    Upload --> PG
    Upload --> MinIO
    AI --> FastAPI["AI Service"]
```

| 包 | 主要职责 |
| --- | --- |
| `module.gallery` | 图库 API 与搜索编排 |
| `module.upload` | PostgreSQL 上传任务、MinIO staging 与 Worker |
| `module.image` | 图片实体/DTO、搜索 SQL、对象 URL、缩略图与文件存储 |
| `module.tag` | 标签字典、标签查询、图片标签关系 |
| `module.ai` | AI HTTP 客户端、持久化 Job、租约 Worker 与结果事务 |
| `module.system` | 首次初始化、JWT 校验与设置持久化 |
| `config` | Web 拦截器、调度、MinIO 与任务属性绑定 |

## HTTP API

所有业务接口都以 `/api` 开头；除认证白名单外，请求由 `AuthInterceptor` 校验 `Authorization: Bearer <token>`。

| 资源 | 代表性接口 | 用途 |
| --- | --- | --- |
| 认证 | `GET /api/auth/status`、`POST /api/auth/setup`、`POST /api/auth/login` | 首次初始化、登录与令牌签发 |
| 搜索 | `POST /api/search`、`POST /api/search/image` | 条件/语义检索、以图搜图 |
| 图片 | `GET/PUT/DELETE /api/images/{id}` | 详情、编辑、删除 |
| 图片标签 | `POST/DELETE /api/images/{id}/tags/{tagId}` | 手工维护标签 |
| AI 管理 | `POST /api/images/{id}/ai/retry` | 重试已经终止失败的单图任务 |
| 批量操作 | `POST /api/images/batch/delete`、`POST /api/images/batch/download` | 批量删除、ZIP 下载 |
| 上传 | `POST /api/upload`、`GET/POST/DELETE /api/upload/tasks` | 创建、查看、重试、清理上传任务 |
| 标签/设置 | `GET /api/tags`、`GET/POST /api/system/settings` | 标签检索与运行时设置 |

开发环境可通过 Springdoc 页面查看由控制器注解生成的完整接口定义：`/swagger-ui/index.html`。

## 上传任务

```mermaid
flowchart TD
    Upload["接收 multipart 文件"] --> Staging["MinIO: staging/{jobId}"]
    Staging --> Queue["PostgreSQL: upload_jobs/PENDING"]
    Queue --> Worker["SKIP LOCKED 领取任务"]
    Worker --> Hash["计算 SHA-256 并查重"]
    Hash --> Meta["解析格式、宽高；拒绝动图"]
    Meta --> Objects["写原图和缩略图"]
    Objects --> Insert["图片元数据入库<br/>PENDING"]
    Insert --> Async["同一事务创建 ai_jobs/PENDING"]
    Worker -->|"异常"| Failed["upload_jobs/FAILED<br/>保留 staging"]
    Failed -->|"手动重试"| Queue
```

任务状态保存在 `upload_jobs`。Worker 原子领取任务后写入 `locked_by` 与 `locked_until`，并通过心跳续租；崩溃后租约过期的任务可重新领取。失败任务保留 staging 对象以支持重试；成功任务删除 staging，对应任务记录默认保留 7 天用于追踪。

## AI 后处理状态机

```mermaid
stateDiagram-v2
    [*] --> PENDING: 图片与 ai_job 原子创建
    PENDING --> PROCESSING: SKIP LOCKED 领取
    PROCESSING --> READY: 推理结果与 Job 原子完成
    PROCESSING --> PENDING: 可重试失败 / 指数退避
    PROCESSING --> FAILED: 第 5 次失败
    FAILED --> PENDING: 详情页手动重试
    PROCESSING --> PROCESSING: 租约心跳
```

- `ai_jobs` 是执行状态事实来源，`images.ai_status` 是面向查询和前端的事务性投影。
- Worker 使用 `FOR UPDATE SKIP LOCKED`、五分钟租约和心跳；崩溃后由其他实例领取过期任务。
- 默认最多尝试五次，按 30 秒起始的指数退避重试；终止失败后只能由详情页手动重置。
- 打标阈值来自运行时设置 `tag.threshold`。
- 完成阶段在事务中写入图像 `vector(512)`、新标签关系与时间戳。
- 完成和失败提交都会校验 `locked_by`，失去租约的旧 Worker 不能覆盖新结果。

## 搜索实现

`SearchService` 负责选择检索路径，`ImageSearchService` 使用 JdbcTemplate/native SQL 完成过滤和排序。

- 条件检索支持标签、关键字、AI 状态、宽高、文件大小、排序和随机种子。
- `semanticQuery` 先调用 AI Service 生成 CLIP 文本向量，再用 pgvector 距离排序。
- 以图搜图把 multipart 文件直接转发给 AI Service 生成视觉向量，不创建临时 MinIO 对象。
- 查询使用 `LIMIT size + 1` 计算 `hasNext`，响应不包含精确总数。
- 列表 DTO 只包含展示所需字段和可推导的 MinIO URL，降低对象存储访问次数。

## 配置与持久化

Flyway 在启动时执行 `src/main/resources/db/migration`。Hibernate 使用 `ddl-auto: validate`，因此表结构变更应新增 migration，而不是依赖实体自动改表。

运行时设置保存在 `system_settings` 并由 Web Service 直接读取。缩略图规格、上传 Worker 和 AI Job Worker 参数属于部署配置，来自 `application.yml` 对应的环境变量，不属于运行时设置。
