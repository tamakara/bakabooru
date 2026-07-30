# 运维手册

本页按“先判断阶段，再处理失败”的方式组织。上传入库与 AI 后处理是两个独立阶段，排障时不要把 `upload_jobs` 和图片 `ai_status` 混为一谈。

## 运行状态总览

```mermaid
flowchart LR
    Upload["浏览器上传"] --> Staging["MinIO staging"]
    Staging --> Jobs["PostgreSQL upload_jobs"]
    Jobs --> Archive["原图/缩略图归档"]
    Archive --> Pending["图片 PENDING"]
    Pending --> Processing["图片 PROCESSING"]
    Processing --> Ready["图片 READY"]

    Jobs -->|"入库失败"| UploadFailed["FAILED + staging"]
    Processing -->|"重试耗尽"| AiFailed["FAILED + ai_error"]
    UploadFailed -->|"上传页重试"| Jobs
    AiFailed -->|"详情页重试"| Processing
```

## 日常检查

```bash
docker compose ps
docker compose logs --tail 200 backend-web-service
docker compose logs --tail 200 ai-service
```

重点观察：

| 组件 | 健康/异常信号 |
| --- | --- |
| Web Service | `/actuator/health`、数据库迁移、上传 Worker、MinIO 连接、AI 处理日志 |
| AI Service | `/health` 的 `loading/ok`、模型下载、CUDA Provider、推理异常 |
| PostgreSQL | `pg_isready`、Flyway migration、磁盘空间 |
| MinIO | bucket 是否存在、`original/` 与 `thumbnail/` 对象、磁盘空间 |

Compose 的 AI 健康检查只要求 `/health` 可访问。判断模型是否真正就绪时，应查看响应体是否为 `{"status":"ok"}` 或检查 AI Service 日志中的“所有模型预加载完成”。

## AI 状态与恢复

```mermaid
stateDiagram-v2
    PENDING --> PROCESSING: Worker 领取
    PROCESSING --> READY: 成功
    PROCESSING --> PENDING: 可重试失败 / 指数退避
    PROCESSING --> FAILED: 第 5 次失败
    FAILED --> PENDING: 手动重试
```

| 状态 | 解释 | 建议操作 |
| --- | --- | --- |
| `PENDING` | 等待首次处理或自动退避 | 查看 `ai_jobs.next_retry_at`，通常无需人工操作 |
| `PROCESSING` | Worker 已持有租约并调用 AI | 观察 Web/AI 日志；实例退出后租约过期可恢复 |
| `READY` | 标签和图像向量已写入 | 可参与完整的向量检索 |
| `FAILED` | 五次尝试均失败 | 修复根因后在详情页单图重试 |

自动恢复依赖数据库租约而不是启动扫描。Worker 通过 `FOR UPDATE SKIP LOCKED` 领取到期任务，失败按 30 秒起始的指数退避自动重试；第五次失败才写入图片 `ai_error` 并停止。

## 上传任务恢复

上传页展示 PostgreSQL 待处理数、当前正在处理的任务和失败任务列表。

- 单个失败任务会保留 MinIO staging 对象，可重新设为 `PENDING`。
- 清空失败任务会同时删除 staging 对象与 PostgreSQL 任务记录。
- Worker 使用两分钟锁租约并定期续期；实例崩溃后，租约过期的任务会被重新领取。
- 已完成任务默认保留 7 天，之后由定时清理任务删除。
- Web Service 当前按顺序做文件入库，大文件缩略图生成可能让任务短时堆积，这是预期行为。

## 缩略图 Backfill

Web Service 启动后在后台检查当前配置路径的缩略图，缺失时从原图补生成。此过程不阻塞搜索。

```mermaid
flowchart TD
    Start["应用启动"] --> Scan["扫描图片记录"]
    Scan --> Exists{"当前规格缩略图存在?"}
    Exists -->|"是"| Next["下一张"]
    Exists -->|"否"| Original["从 MinIO 读取原图"]
    Original --> Generate["按 maxSize / quality / format 生成"]
    Generate --> Upload["上传当前规格缩略图"]
    Upload --> Next
```

缩略图规格是应用启动配置。修改尺寸或格式后会形成新的对象前缀，旧规格不会自动清理；确认新规格全部补齐并完成备份后，再制定单独的对象清理方案。

## 常见故障

### 图片列表存在，但缩略图 404

1. 前端会自动回退到原图，因此通常不影响浏览。
2. 查看 Web Service 的 backfill 日志。
3. 检查 `original/{hash}` 是否存在、MinIO 凭据是否一致、bucket 是否可读。
4. 确认 Nginx `/oss/*` 代理与 MinIO bucket 路径匹配。

### AI 长时间处于 `PENDING`

1. 打开图片详情查看 `aiError`。
2. 检查 AI `/health` 是否已从 `loading` 变为 `ok`。
3. 检查模型缓存下载、CUDA/CPU Provider 和 MinIO 原图读取。
4. `FAILED` 时修复根因后单图重试；`PENDING` 会自动继续处理。

### 语义搜索或以图搜图结果少

- 只有 `embedding IS NOT NULL` 的图片参与向量排序。
- 用 `READY` 状态过滤确认已完成处理的数据量。
- 检查 AI Service 文本/视觉 CLIP 模型是否来自同一模型版本。
- 以图搜图还受请求中的相似度阈值影响，可在确认需求后适当降低。

### 普通搜索可用，但语义搜索失败

这通常说明 Web Service 与数据库正常，而 AI Service 尚未就绪或内部 URL 不通。检查应用中的 AI 服务地址覆盖、Compose 网络、模型状态和 AI 日志；无需先重启 PostgreSQL 或 MinIO。

## 备份建议

```mermaid
flowchart LR
    PG[("PostgreSQL 备份")] --> Set["同一恢复点"]
    MinIO[("MinIO data 备份")] --> Set
    Cache["模型缓存，可选"] -.-> Set
```

图片元数据、上传任务与对象文件必须尽量采用同一恢复点。模型缓存可以重新下载。
