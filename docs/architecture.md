# 系统架构

BaKaBooru 采用本地优先、业务与推理解耦的服务架构。浏览器只访问 Nginx；Nginx 提供前端资源并代理 `/api/*` 与 `/oss/*`。Web Service 是业务数据的唯一写入入口，AI Service 负责模型推理，仅标签向量初始化会直接更新 PostgreSQL。

## 容器与依赖

```mermaid
flowchart LR
    User(("用户")) -->|"HTTP :80"| Nginx

    subgraph App["应用层"]
        Nginx["Frontend<br/>Nginx + Vue 3"]
        Web["Web Service<br/>Spring Boot 3 / Java 21"]
        AI["AI Service<br/>FastAPI + ONNX Runtime"]
    end

    subgraph Data["数据层"]
        PG[("PostgreSQL 16<br/>pgvector")]
        MinIO[("MinIO<br/>original + thumbnail")]
        Redis[("Redis<br/>上传队列 + 设置缓存")]
        Cache[("模型缓存<br/>data/model_cache")]
    end

    Nginx -->|"/api/*"| Web
    Nginx -->|"/oss/*"| MinIO
    Web -->|"JPA / SQL / Flyway"| PG
    Web -->|"S3 API"| MinIO
    Web -->|"队列 / Hash 缓存"| Redis
    Web -->|"内部 HTTP"| AI
    AI -->|"读取原图"| MinIO
    AI -.->|"仅初始化标签向量"| PG
    AI -->|"加载模型"| Cache
```

## 服务边界

| 组件 | 负责 | 不负责 |
| --- | --- | --- |
| Frontend | 页面、交互、查询缓存、上传进度、令牌携带 | 直接访问数据库或推理服务 |
| Web Service | 鉴权、业务 API、事务、元数据、对象路径、上传队列、AI 调度 | 执行模型推理 |
| AI Service | Camie Tagger、CLIP 文本/图像向量、标签向量初始化 | 图片业务状态、上传任务、鉴权 |
| PostgreSQL | 图片、标签、关系、设置、向量与索引 | 图片二进制文件 |
| MinIO | 原图与固定规格缩略图 | 图片元数据与状态 |
| Redis | 上传任务队列/任务数据、失败队列、系统设置缓存 | 最终业务事实来源 |

## 上传与 AI 后处理

上传入库由单线程任务消费者处理；入库成功后，AI 后处理进入独立的 `aiExecutor` 线程池。两阶段分离，因此图片可以先出现在图库中，再异步转为 `READY`。

```mermaid
sequenceDiagram
    autonumber
    actor U as 用户
    participant F as Frontend
    participant W as Web Service
    participant R as Redis
    participant M as MinIO
    participant P as PostgreSQL
    participant A as AI Service

    U->>F: 选择图片
    F->>W: POST /api/upload
    W->>W: 保存临时文件
    W->>R: 写任务数据并入队
    W-->>F: 上传请求完成

    W->>R: 阻塞弹出任务
    W->>W: SHA-256、查重、解析尺寸
    W->>M: 写 original/{hash}
    W->>M: 写 thumbnail/{size}/{hash}.{format}
    W->>P: 插入图片，ai_status=PENDING
    W->>W: aiExecutor 异步调度
    W->>P: ai_status=PROCESSING
    W->>A: 标签识别 + 图像 CLIP 向量
    A->>M: 读取 original/{hash}
    A-->>W: 标签分数 + vector(512)
    W->>P: 写标签关系、embedding、READY
```

失败点分属两个队列语义：文件入库失败会进入 Redis 失败队列，可从上传页重试；AI 推理失败会让图片回到 `PENDING` 并记录 `ai_error`，可从详情页重试。

## 检索路径

```mermaid
flowchart TD
    Request["POST /api/search"] --> Mode{"检索模式"}
    Mode -->|"标签 / 关键字 / 元数据"| SQL["动态 SQL 过滤与排序"]
    Mode -->|"semanticQuery"| Text["AI Service<br/>CLIP 文本向量"]
    Mode -->|"POST /api/search/image"| Vision["AI Service<br/>CLIP 图片向量"]
    Text --> Vector["pgvector 余弦距离排序"]
    Vision --> Vector
    SQL --> Page["LIMIT size + 1"]
    Vector --> Page
    Page --> DTO["轻量 ImageThumbnailDto<br/>content + page + size + hasNext"]
    DTO --> Frontend["缩略图展示<br/>失败时回退原图"]
```

- 普通筛选不依赖 AI Service。
- 语义搜索和以图搜图只检索 `embedding IS NOT NULL` 的图片。
- 分页通过多取一条计算 `hasNext`，不执行精确总数统计。
- 列表返回确定性的对象 URL，不逐条访问 MinIO 做存在性检查。

## 一致性与可用性

- PostgreSQL 是图片元数据、标签关系和运行时设置的事实来源。
- Redis 中的设置缓存在 Web Service 启动时由数据库预热；缓存缺失时会回源。
- AI Service 的 `/health` 表示进程可访问，并通过 `status=loading|ok` 暴露模型状态；推理接口在模型未就绪时返回 `503`。
- Web Service 不依赖 AI Service 健康检查启动，因此 AI 模型加载期间仍可登录、浏览和进行非语义检索。
