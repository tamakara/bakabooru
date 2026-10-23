# 部署与配置

本文面向 Docker Compose 部署者，说明服务、环境变量、持久化目录和升级方式。首次运行见[快速开始](getting-started.md)，故障与恢复见[运维手册](operations.md)。

## 服务拓扑

Compose 默认网络按服务名提供 DNS：`frontend` 调用 `backend`，应用连接 `postgres`、`minio` 和 `model-manager`。

| 服务 | 作用 | 端口/数据 |
| --- | --- | --- |
| `frontend` | 页面和 `/api`、`/oss` 代理 | `80` |
| `backend` | 业务 API、持久化任务 | 内部 `8080` |
| `model-manager` | CUDA 推理和模型管理 | 内部 `8000`；`data/model_cache` |
| `postgres` | PostgreSQL 16 + pgvector | 内部 `5432`；`data/postgres` |
| `minio` | 图片对象存储 | `9000`、`9001`；`data/minio` |
| `minio-createbuckets` | 创建 `images` bucket 并启用匿名读取 | 一次性初始化任务 |

模型服务需要 NVIDIA Driver、NVIDIA Container Toolkit 和可用 GPU。`model-manager` 不作为 `backend` 的启动依赖；非语义功能可在模型加载时使用。服务边界见[架构设计](architecture.md)。

## 配置

Compose 未提供 `.env` 时会采用开发默认值。建议复制 `.env.example` 并替换默认凭据：

```bash
cp .env.example .env
```

| 变量 | 用途 | 默认值 |
| --- | --- | --- |
| `POSTGRES_USER` | 数据库用户 | `bakabooru` |
| `POSTGRES_PASSWORD` | 数据库密码 | `change-me` |
| `POSTGRES_DB` | 数据库名 | `bakabooru` |
| `MINIO_ROOT_USER` | MinIO root 用户及应用访问凭据 | `bakabooru` |
| `MINIO_ROOT_PASSWORD` | MinIO root 密码及应用访问凭据 | `change-me` |

`.env` 只用于 Compose 注入基础设施凭据，不应提交到版本库。应用地址和 Worker 默认值见 `backend/src/main/resources/application.yml` 与 `model-manager/app/core/settings.py`。可通过 `MODEL_MANAGER_URL` 覆盖 Backend 到模型服务的地址；标签阈值、重试策略等运行时设置由 UI 管理。

MinIO bucket 当前配置为匿名可读，以支持浏览器图片代理。对外部署应在网络层限制 MinIO 管理入口；改为私有 bucket 需要同步实现鉴权图片代理或签名 URL。

## 升级与持久化

```bash
docker compose up -d --build --wait
```

数据库 migration 会在 Backend 启动时应用。服务名已统一为 `backend` 和 `model-manager`；Compose 会按新服务配置重建容器，数据目录保持不变。凭据变更不会自动重写已有 PostgreSQL/MinIO 数据目录中的账号。

`data/postgres`、`data/minio` 和 `data/model_cache` 是宿主持久化目录。数据库和 MinIO 对象应成对备份；模型缓存可重新下载。备份/恢复见[运维手册](operations.md)。
