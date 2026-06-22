# Frontend

前端位于 `frontend`，使用 Vue 3、Naive UI、Pinia 和 TanStack Query。

## 页面

- GalleryView: 搜索、筛选、分页、批量操作、图搜。
- UploadView: 上传队列、失败上传重试。
- SettingsView: 系统设置。
- ImageDetail: 图片详情、标签编辑、AI 状态和手动重试。

## 搜索体验

搜索结果使用 `SearchResult<T>`：

- `content`: 当前页结果。
- `page`: 当前页号。
- `size`: 页大小。
- `hasNext`: 是否有下一页。

前端不再显示精确总数，改为上一页/下一页。搜索侧栏提供 AI 状态筛选：

- 全部
- 待处理
- 处理中
- 已完成

## 图片详情

详情页展示图片元数据、标签、AI 状态和错误。状态为 `PENDING` 且存在错误时，可以点击“重试”重新触发 AI 后处理。

## 缩略图兜底

列表优先加载缩略图 URL。如果当前配置尺寸的缩略图尚未 backfill 完成，图片加载失败后会自动 fallback 到原图 URL。
