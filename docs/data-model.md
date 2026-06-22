# Data Model

## images

保存图片元数据和 CLIP 图像向量。

关键字段：

- `id`: 图片 ID。
- `file_name`: 原始文件名。
- `hash`: SHA-256，作为对象存储主键。
- `embedding`: `vector(512)`，图像 CLIP embedding。
- `ai_status`: `PENDING`、`PROCESSING`、`READY`。
- `ai_error`: 最近一次 AI 处理错误。
- `ai_attempted_at`: 最近一次开始 AI 处理时间。
- `ai_completed_at`: 最近一次完成 AI 处理时间。

对象路径：

- 原图：`original/{hash}`
- 缩略图：`thumbnail/{maxSize}/{hash}.{format}`

## tags

保存标签字典。

关键字段：

- `name`: 标签名。
- `type`: 标签类型。
- `embedding`: `vector(384)`，用于标签语义匹配或扩展能力。

## image_tag_relation

保存图片和标签的多对多关系。

关键字段：

- `image_id`
- `tag_id`
- `score`: AI 置信度或手动标签分数。

## system_settings

保存少量运行时设置，例如上传轮询间隔和标签阈值。缩略图尺寸不放在这里，改由配置文件和环境变量控制。

## 索引

- `idx_images_embedding`: pgvector HNSW 图像向量索引。
- `idx_images_ai_status`: AI 状态过滤。
- `idx_image_tag_relation_tag_image`: 标签过滤。
- `idx_images_created_at`: 默认排序。
- `idx_images_size`、`idx_images_dimensions`: 元数据过滤。
