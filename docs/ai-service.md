# AI Service

AI Service 位于 `backend/ai_service`，使用 FastAPI。它只负责模型推理，不保存业务状态。

## 能力

- 图像打标：Camie Tagger。
- 文本语义向量：CLIP text encoder。
- 图像相似向量：CLIP vision encoder。
- MinIO 图片读取：用于入库后的原图 embedding。
- Multipart 图片 embedding：用于以图搜图，避免临时对象绕路。

## 接口

- `POST /tag/image`: 输入 MinIO object name 和阈值，返回标签分数。
- `POST /search/embedding`: 输入 query，返回 CLIP 文本向量。
- `POST /embedding/image`: 输入 MinIO object name，返回图像向量。
- `POST /embedding/image-file`: 输入 multipart 文件，返回图像向量。
- `GET /health`: 仅表示 HTTP 服务可用。

## 模型加载

服务启动时预加载所有模型（CLIP、FastEmbed、CamieTagger），加载完成后才标记为就绪。模型未加载完成时，所有推理接口返回 503。

## 非目标

- 不调用在线 LLM。
- 不做业务状态管理。
- 不直接写图片表或标签关系表。
