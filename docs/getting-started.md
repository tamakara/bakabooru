# 快速开始

本文用于在一台支持 NVIDIA GPU 的主机上首次运行 BaKaBooru。它只覆盖准备、启动、初始化和关闭；完整配置见[部署说明](deployment.md)，问题处理见[运维手册](operations.md)。

## 环境要求

- Docker Engine 或 Docker Desktop，支持 Docker Compose v2。
- NVIDIA Driver 与 NVIDIA Container Toolkit，供 Model Manager 使用 CUDA。
- 能访问容器镜像仓库和 Hugging Face 模型仓库的网络。

## 启动

在项目根目录执行：

```bash
cp .env.example .env
```

修改 `.env` 中的 PostgreSQL 和 MinIO 密码，再启动：

```bash
docker compose up -d --build --wait
docker compose ps
```

首次启动需要构建镜像、执行数据库迁移并下载模型，等待时间取决于网络与 GPU。`--wait` 在服务健康检查通过后返回。

打开 `http://localhost`，按页面提示完成首次密码设置。默认入口如下：

| 地址 | 用途 |
| --- | --- |
| `http://localhost` | BaKaBooru |
| `http://localhost:9001` | MinIO 控制台 |
| `http://localhost:9000` | MinIO S3 API |

## 常用操作

```bash
docker compose logs -f backend model-manager
docker compose stop
docker compose start
docker compose down
```

`down` 会移除容器和网络，不会删除 `data/` 中的数据库、图片或模型缓存。不要使用 `docker compose down -v` 清除仍需保留的数据。

首次登录后可在设置页安装所需模型。上传图片后，文件入库和模型分析在后台异步执行。功能和 HTTP 接口见[功能与 API](features-api.md)。
