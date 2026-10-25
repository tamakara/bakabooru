# BaKaBooru

BaKaBooru 是一个基于 Docker Compose 的自托管图库系统，支持图片归档、标签管理、自动分析、CLIP 语义搜索和以图搜图。图片、业务数据和模型缓存均保留在用户自己的基础设施中。

![Java](https://img.shields.io/badge/Java-21-b07219?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6db33f?style=flat-square&logo=springboot)
![Python](https://img.shields.io/badge/Python-3.12-3776ab?style=flat-square&logo=python)
![Vue.js](https://img.shields.io/badge/Vue.js-3.4-4FC08D?style=flat-square&logo=vuedotjs)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql)

## 能力

- 上传后自动计算 SHA-256、查重、解析元数据、归档原图和生成缩略图。
- 使用模型服务生成标签、图像向量和文本向量。
- 按标签、关键词、状态、尺寸、文件大小和模型向量筛选图片。
- 支持语义搜索、以图搜图、批量分析和失败重试。
- 支持模型目录查看、模型下载，以及分别启用一个标签模型和一个 CLIP 模型。

## 快速启动

需要 Docker Compose v2；模型推理需要 NVIDIA Driver、NVIDIA Container Toolkit 和可用 GPU。

```bash
cp .env.example .env
docker compose up -d --build --wait
docker compose ps
```

访问 `http://localhost`，首次进入时设置密码。首次启动会执行数据库迁移并加载模型，耗时取决于网络和 GPU。

MinIO bucket 默认允许匿名读取图片对象；对外部署前请阅读[部署说明](docs/deployment.md)中的访问边界提示。

## 项目结构

```text
bakabooru/
├── backend/               # Spring Boot API、业务模块、异步任务和 Flyway migration
├── model-manager/         # FastAPI 推理和模型生命周期服务
├── frontend/              # Vue 3、TypeScript、Vite 前端
├── docs/                  # 架构、接口、项目结构和部署文档
├── data/                  # Compose 持久化数据目录
└── docker-compose.yml     # 服务编排
```

Backend 模块和边界见[项目结构](docs/project-structure.md)。

配置由 PostgreSQL 持久化、Redis 缓存，Redis 只由 Backend 使用。model-manager 通过 Backend 内部 HTTP 配置接口读取运行时配置。

## 文档

| 文档 | 内容 |
| --- | --- |
| [架构设计](docs/architecture.md) | 服务边界、模块职责、请求流和异步处理 |
| [功能与 API](docs/features-api.md) | 前端功能、Backend API 和 Model Manager API |
| [数据模型](docs/data-model.md) | PostgreSQL 表、关系、状态和任务生命周期 |
| [项目结构](docs/project-structure.md) | 目录、模块边界和本地验证入口 |
| [部署与配置](docs/deployment.md) | Compose、环境变量、持久化、升级和备份 |
