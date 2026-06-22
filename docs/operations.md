# Operations

## AI 状态

- `PENDING`: 图片已入库，但 AI 未完成。若 `aiError` 非空，表示上次处理失败。
- `PROCESSING`: 正在执行标签识别和 CLIP embedding。
- `READY`: AI 处理完成，可参与相似度检索。

## 手动重试

图片详情页提供 AI 重试入口。仅当图片状态为 `PENDING` 且存在 `aiError` 时显示。重试会：

1. 清空 `aiError`。
2. 将状态置为 `PROCESSING`。
3. 异步执行标签识别和 embedding。
4. 成功后置为 `READY`，失败后回到 `PENDING` 并记录错误。

## 批量处理

图库侧栏提供"处理所有待处理图片"按钮，可一键将所有 `PENDING` 且无 `aiError` 的图片入队进行 AI 后处理。适用于：

- 首次部署后批量处理历史图片。
- AI Service 恢复后批量补处理。
- 手动清空错误后重新触发。

## 自动处理

上传完成后会自动触发 AI 后处理。应用启动时会扫描 `PENDING` 且无 `aiError` 的图片并入队，避免失败图片被无限自动重试。

## 模型预加载

AI Service 启动时会预加载所有模型（CLIP、FastEmbed、CamieTagger），加载完成后才标记为就绪。模型未加载完成时，所有推理接口返回 503。Web Service 不阻塞启动，AI 未就绪时普通浏览和搜索仍可用。

## 缩略图 Backfill

启动后后台线程会检查当前配置路径下的缩略图是否存在，缺失则从 original 生成。这个流程不阻塞搜索。

## 常见问题

搜索结果有图但缩略图短暂 404：

- 原因：当前尺寸缩略图尚未 backfill。
- 影响：前端会 fallback 到原图。

语义搜索结果少：

- 只有 `embedding IS NOT NULL` 的图片参与相似度检索。
- 可筛选 `READY` 查看已完成 AI 处理的图片。

AI 一直 PENDING：

- 查看详情页 `aiError`。
- 检查 AI Service 日志和模型缓存。
- 在详情页手动重试。
