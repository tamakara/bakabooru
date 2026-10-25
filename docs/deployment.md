# 部署与配置

## 环境要求

- Docker Engine 或 Docker Desktop，支持 Docker Compose v2。
- NVIDIA Driver、NVIDIA Container Toolkit 和可用 GPU。
- 可访问容器镜像仓库和模型仓库的网络。

## 服务

| 服务 | 作用 | 数据/端口 |
| --- | --- | --- |
| `frontend` | Vue 页面和 `/api`、`/oss` 代理 | `80` |
| `backend` | Spring Boot API、数据库迁移和后台任务 | 内部 `8080` |
| `model-manager` | FastAPI 推理和模型生命周期 | 内部 `8000`、`data/model_cache` |
| `postgres` | PostgreSQL 16 + pgvector | `data/postgres`、内部 `5432` |
| `redis` | Backend 内部配置缓存 | `data/redis`、内部 `6379`；仅 Backend 配置连接 |
| `minio` | 图片对象存储 | `data/minio`、`9000/9001` |

## 启动和升级

```bash
cp .env.example .env
# 修改 .env 中的凭据
docker compose up -d --build --wait
docker compose ps
```

浏览器访问 `http://localhost`。首次启动会执行 Flyway migration 并加载模型。升级使用同一条 `docker compose up -d --build --wait` 命令，数据库迁移会在 Backend 启动时自动执行。

## 环境变量

| 变量 | 用途 | 示例默认值 |
| --- | --- | --- |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | PostgreSQL 凭据和数据库名 | `bakabooru_user` / `bakabooru_password` / `bakabooru` |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | MinIO 和 Backend 访问凭据 | `bakabooru_user` / `bakabooru_password` |
| `MINIO_BUCKET_NAME` | 图片 bucket | `images` |
| `DATA_DIR` | 持久化数据根目录 | `./data` |
| `REDIS_PASSWORD` | Redis 密码，由 Backend 和 Redis 容器使用 | `bakabooru_password` |

Backend 的服务地址、Worker 调度参数和缩略图规格位于 `backend/src/main/resources/application.yml`；Model Manager 的 Backend 地址、CUDA 和模型缓存目录位于 `model-manager/app/core/settings.py`。标签阈值、分析重试参数、上传任务保留天数和推理并发通过系统设置接口修改，PostgreSQL 持久化、Redis 仅由 Backend 缓存。

Backend 启动时会确保 MinIO bucket 存在并设置为匿名可读，以便浏览器加载图片。MinIO 控制台和 S3 API 端口会映射到宿主机；对外部署时应在防火墙或反向代理层限制这些管理入口。改为私有 bucket 需要同时实现鉴权图片代理或签名 URL。

## 持久化和备份

```text
data/
├── postgres/       # PostgreSQL 数据
├── minio/          # 原图、缩略图和暂存文件
├── redis/           # Backend 配置缓存，可丢失并自动从 PostgreSQL 回填
└── model_cache/    # 模型工件，可重新下载
```

数据库和 MinIO 对象应在同一维护窗口备份：

```bash
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > bakabooru.sql
```

模型缓存不是业务数据，可以在恢复后重新下载。不要对仍需保留数据的部署运行 `docker compose down -v`。

## 健康检查和故障定位

```bash
docker compose ps
docker compose logs --tail 200 backend model-manager
```

Backend 健康端点为 `/actuator/health`；Model Manager 在模型未加载完成前 `/health` 返回 503。分析任务由数据库租约驱动，模型服务恢复后会自动重试；图片详情可以手动重新排队失败分析。
