# 运维手册

本文提供日常检查、故障定位、任务恢复与备份操作。部署配置见[部署说明](deployment.md)，图片和任务状态见[数据模型](data-model.md)。

## 健康与日志

```bash
docker compose ps
docker compose logs --tail 200 backend
docker compose logs --tail 200 model-manager
docker compose logs --tail 200 postgres minio
```

Backend 健康端点为容器内 `http://backend:8080/actuator/health`。Model Manager 在模型未加载完成时 `/health` 返回 503，初始化成功后返回 `status=ok`；Compose 会等待健康检查通过。

| 症状 | 检查方向 |
| --- | --- |
| 服务启动失败 | 容器日志、GPU 运行时和磁盘空间 |
| 缩略图 404 | Backend backfill 日志、MinIO 原图和 bucket 路径 |
| AI 任务长期未完成 | Model Manager 健康状态、模型安装状态、CUDA 和 MinIO 读取错误 |
| 语义搜索失败 | `model-manager` 网络连通性及所选 CLIP 模型状态 |
| 图片记录存在但对象缺失 | MinIO 磁盘、bucket 和对象前缀；确认后再清理缺失记录 |

## 任务恢复

上传页展示待处理数量、当前任务和失败任务。单个失败任务可重试；清理失败任务会删除 staging 对象和任务记录。Worker 崩溃时，数据库租约过期后任务可重新领取。已完成上传任务默认保留 7 天。

分析失败会按退避策略重试，超过配置的最大次数后标记为失败。修复模型或对象读取问题后，可从图片详情重新排队。图片总体状态与分析任务状态不同，具体字段见[数据模型](data-model.md)。

## 备份与恢复

为避免数据库引用不存在的对象，PostgreSQL 与 MinIO 应在同一维护窗口备份。数据库容器已注入 Compose 配置的用户和数据库名，可直接导出：

```bash
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > bakabooru.sql
```

MinIO 使用 S3 兼容客户端备份 `images` bucket。模型缓存无需纳入强制备份。

恢复前停止 Backend，恢复数据库与 MinIO 对象后再启动应用。不要对仍需保留数据的部署运行 `docker compose down -v`。

## 缩略图补齐

Backend 启动后会在后台检查当前缩略图规格，对缺失缩略图从原图重新生成。更改尺寸或格式会产生新的对象前缀；旧前缀不会自动删除。确认新规格补齐并完成备份后，再单独清理旧对象。
