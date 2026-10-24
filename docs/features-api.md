# 功能与 API

Backend API 使用 `/api` 前缀。除认证初始化、登录和健康检查外，请求需要 `Authorization: Bearer <token>`。字段细节以运行中的 OpenAPI 为准：`/v3/api-docs` 或 `/swagger-ui/index.html`。

## 功能

| 功能 | 入口 |
| --- | --- |
| 认证 | 首次密码设置、登录和修改密码；`/api/auth/*` |
| 图库搜索 | 标签/关键词/元数据过滤、语义搜索和以图搜图；`/api/search*` |
| 图片管理 | 查看、编辑、删除图片和维护人工标签；`/api/images/*` |
| 图片分析 | 自动或手动排队打标、向量计算、失败重试和批量操作；`/api/images/*/ai/*` |
| 上传任务 | 文件上传、队列状态、失败任务重试和清理；`/api/upload*` |
| 标签 | 标签列表和查询；`/api/tags` |
| 模型管理 | 查看模型状态和提交模型下载/卸载操作；`/api/ai/models` |
| 系统设置 | 标签阈值、重试策略和推理运行时设置；`/api/system/settings` |

搜索条件支持正负标签、关键词、语义文本、图片状态、向量模型、宽高、文件大小、排序和分页。上传接口返回表示文件已进入后台入库队列，不表示图片分析已经完成。

## Backend API

### 认证

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/api/auth/status` | 查看初始化状态 |
| `POST` | `/api/auth/setup` | 首次设置密码 |
| `POST` | `/api/auth/login` | 登录并获取 Bearer token |
| `POST` | `/api/auth/password` | 修改密码 |

### 搜索、标签和图片

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/api/search` | 标签、关键词、元数据或语义搜索 |
| `POST` | `/api/search/image` | multipart 以图搜图；字段 `file`，可选 `threshold`、`page`、`size`、`vectorModelIds`、`vectorModelId` |
| `GET` | `/api/tags?query=` | 查询标签 |
| `GET` | `/api/images/{id}` | 获取图片详情 |
| `PUT` | `/api/images/{id}` | 更新图片元数据 |
| `DELETE` | `/api/images/{id}` | 删除图片及对象 |
| `POST` / `DELETE` | `/api/images/{id}/tags/{tagId}` | 添加或移除标签 |
| `POST` | `/api/images/batch/delete` | 批量删除 |
| `POST` | `/api/images/batch/delete-missing` | 清理缺失对象记录 |
| `POST` | `/api/images/batch/download` | 下载图片 ZIP |

### 图片分析

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/api/images/{id}/ai/retry?capability=` | 重试分析 |
| `POST` | `/api/images/{id}/ai/tags` | 为单张图片排队生成标签；body 含 `modelId` |
| `POST` | `/api/images/{id}/ai/vectors` | 为单张图片排队生成向量；body 含 `modelIds` |
| `POST` | `/api/images/batch/ai/tags` | 批量生成标签；body 含 `ids`、`modelId` |
| `POST` | `/api/images/batch/ai/vectors` | 批量生成向量；body 含 `ids`、`modelIds` |

这些路由由 `ImageAnalysisController` 提供，业务流程位于 `module.analysis`，图片 CRUD 不参与分析任务编排。

### 上传、模型和设置

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/api/upload` | 上传 multipart 图片；字段 `file`、可选 `tagModelId`、`vectorModelIds` |
| `GET` | `/api/upload/tasks` | 获取上传队列和失败任务 |
| `POST` | `/api/upload/tasks?id={uuid}` | 重试上传任务 |
| `DELETE` | `/api/upload/tasks` | 清除失败上传任务 |
| `GET` | `/api/ai/models` | 获取模型目录和状态 |
| `POST` | `/api/ai/models/{id}/download` | 下载模型工件 |
| `DELETE` | `/api/ai/models/{id}/artifact` | 将模型状态设为未安装 |
| `GET` | `/api/system/settings` | 获取系统设置 |
| `GET` | `/api/system/settings/metadata` | 获取设置元数据 |
| `POST` | `/api/system/settings` | 更新可编辑设置 |

## Model Manager API

这些接口只供 Backend 调用，默认不经前端公开代理。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/health` | 服务和模型健康状态 |
| `GET` | `/v1/models` | 模型目录 |
| `POST` | `/v1/models/{model_id}/download` | 下载模型 |
| `POST` | `/v1/images/analyze` | 同时执行标签和向量分析 |
| `POST` | `/v1/images/tags` | 生成标签 |
| `POST` | `/v1/images/vectors` | 生成图像向量 |
| `POST` | `/v1/embeddings/text` | 生成文本向量 |
| `POST` | `/v1/embeddings/image-file` | 从上传图片生成向量 |
| `POST` | `/v1/embeddings/image` | 从 MinIO 对象生成向量 |
| `POST` | `/v1/runtime-settings` | 更新推理并发参数 |
