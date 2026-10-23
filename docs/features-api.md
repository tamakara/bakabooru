# 功能与 API

本文是面向使用者和 API 调用方的功能目录，并列出 Backend 与 Model Manager 的 HTTP 接口。Backend 接口由 `/api` 提供；除认证初始化、登录和健康检查外，请求需要携带 `Authorization: Bearer <token>`。接口字段和响应结构以运行中的 OpenAPI 为准。

## 功能目录

| 功能 | 说明 | 主要接口 |
| --- | --- | --- |
| 账户 | 首次设置密码、登录、修改密码 | `/api/auth/*` |
| 图库搜索 | 标签、关键词、元数据筛选、语义搜索、以图搜图 | `/api/search` |
| 图片管理 | 查看、编辑、删除图片；维护标签 | `/api/images/*` |
| 图片分析 | 排队打标或向量计算，支持失败重试和批量操作 | `/api/images/*/ai/*` |
| 上传 | 上传图片、查看队列、重试或清理失败任务 | `/api/upload*` |
| 标签 | 列表与前缀查询 | `/api/tags` |
| 模型 | 查看模型目录、下载/安装模型、删除本地模型文件 | `/api/ai/models` |
| 系统设置 | 读取设置及元数据、保存可编辑设置 | `/api/system/settings` |

搜索支持正负标签、关键词、语义文本、图片状态、向量模型、宽高和文件大小范围、排序及分页。结果使用 `content`、`page`、`size`、`hasNext`，不提供精确总数。上传成功表示文件已进入后台入库队列，不代表分析已经完成。

## Backend 接口

### 认证

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/api/auth/status` | 查看是否已初始化 |
| `POST` | `/api/auth/setup` | 首次设置密码 |
| `POST` | `/api/auth/login` | 使用密码获取 Bearer token |
| `POST` | `/api/auth/password` | 修改密码 |

### 搜索与标签

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/api/search` | 条件或语义搜索；JSON body 为搜索条件 |
| `POST` | `/api/search/image` | multipart 图片相似搜索；字段 `file`，可传 `threshold`、`page`、`size`、`vectorModelIds` |
| `GET` | `/api/tags?query=` | 查询标签；省略 `query` 时返回标签列表 |

### 图片与分析

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/api/images/{id}` | 图片详情 |
| `PUT` | `/api/images/{id}` | 更新图片元数据 |
| `DELETE` | `/api/images/{id}` | 删除图片及关联数据/对象 |
| `POST` / `DELETE` | `/api/images/{id}/tags/{tagId}` | 添加或移除人工标签 |
| `POST` | `/api/images/{id}/ai/retry?capability=` | 重试失败分析；能力参数可省略 |
| `POST` | `/api/images/{id}/ai/tags` | 排队生成标签 |
| `POST` | `/api/images/{id}/ai/vectors` | 排队生成向量 |
| `POST` | `/api/images/batch/ai/tags` | 批量排队生成标签；body 含 `ids`、可选 `modelId` |
| `POST` | `/api/images/batch/ai/vectors` | 批量排队生成向量；body 含 `ids`、可选 `modelIds` |
| `POST` | `/api/images/batch/delete` | 批量删除，body 为图片 ID 数组 |
| `POST` | `/api/images/batch/delete-missing` | 清理缺失对象对应的图片记录 |
| `POST` | `/api/images/batch/download` | 下载所选图片 ZIP，body 为图片 ID 数组 |

### 上传、模型和设置

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/api/upload` | 上传 multipart 图片，字段 `file`；可选 `tagModelId`、`vectorModelIds` |
| `GET` | `/api/upload/tasks` | 获取待处理数量、当前任务和失败任务 |
| `POST` | `/api/upload/tasks?id={uuid}` | 重试失败上传任务 |
| `DELETE` | `/api/upload/tasks` | 清除失败上传任务 |
| `GET` | `/api/ai/models` | 获取模型目录和安装状态 |
| `POST` | `/api/ai/models/{id}/download` | 安装/下载模型 |
| `DELETE` | `/api/ai/models/{id}/artifact` | 删除模型文件 |
| `GET` | `/api/system/settings` | 获取可编辑设置 |
| `GET` | `/api/system/settings/metadata` | 获取设置说明、默认值及当前值 |
| `POST` | `/api/system/settings` | 保存可编辑设置，JSON key/value 对象 |

### 查询接口定义

Backend OpenAPI 位于 `/v3/api-docs`，Swagger UI 位于 `/swagger-ui/index.html`。Model Manager 内部接口文档位于 `http://model-manager:8000/docs`，不经前端公开代理。

## Model Manager 内部接口

这些接口供 Backend 调用，不是浏览器公共 API。推理接口需要模型已初始化；服务就绪后 `GET /health` 返回 `status=ok`。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/health` | 服务及模型健康状态 |
| `GET` | `/v1/models` | 获取模型目录 |
| `POST` | `/v1/models/{model_id}/download` | 下载模型工件 |
| `POST` | `/v1/images/analyze` | 一次读取图片并执行打标/向量分析 |
| `POST` | `/v1/images/tags` | 仅生成标签 |
| `POST` | `/v1/images/vectors` | 生成图像向量 |
| `POST` | `/v1/embeddings/text` | 生成文本向量 |
| `POST` | `/v1/embeddings/image-file` | 从 multipart 图片生成图像向量 |
| `POST` | `/v1/embeddings/image` | 从 MinIO 对象生成图像向量 |
| `POST` | `/v1/runtime-settings` | 更新推理并发等运行时参数 |
