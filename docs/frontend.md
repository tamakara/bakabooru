# 前端

前端位于 `frontend`，使用 Vue 3、TypeScript、Vite、Naive UI、Pinia 和 TanStack Vue Query。生产镜像由 Node 构建静态资源，再交给 Nginx 提供页面和反向代理。

## 页面与路由

```mermaid
flowchart TB
    App["App.vue"] --> Router["Vue Router<br/>Memory History"]
    Router --> Login["/login<br/>登录 / 首次初始化"]
    Router --> Layout["MainLayout"]
    Layout --> Gallery["/gallery<br/>搜索、筛选、批量操作"]
    Layout --> Upload["/upload<br/>上传与失败重试"]
    Layout --> Settings["/settings<br/>运行时设置与密码"]
    Gallery --> Detail["ImageDetail<br/>详情、标签、AI 重试"]
    Gallery --> TagInput["TagSearchInput"]
```

项目当前使用 `createMemoryHistory()`，地址栏不会形成传统 history 路径；Nginx 仍配置了 SPA fallback，便于静态入口统一返回 `index.html`。

## 数据访问

```mermaid
flowchart LR
    View["Vue Views / Components"] --> Query["TanStack Vue Query<br/>服务端状态与刷新"]
    View --> Pinia["Pinia Stores<br/>队列设置 / 设置 / 主题"]
    Query --> API["src/api<br/>类型化请求封装"]
    Pinia --> API
    API --> Axios["Axios Client<br/>/api baseURL + Bearer token"]
    Axios --> Nginx["Nginx /api/*"]
    Nginx --> Web["Web Service"]
```

| API 文件 | 对应能力 |
| --- | --- |
| `api/auth.ts` | 初始化状态、登录、设置/修改密码 |
| `api/search.ts` | 条件/语义搜索、以图搜图 |
| `api/gallery.ts` | 图片详情、标签、AI 重试、批量操作 |
| `api/upload.ts` | 文件上传、任务轮询、失败重试与清理 |
| `api/system.ts` | 运行时设置 |
| `api/tags.ts` | 标签联想查询 |

## 搜索与分页

搜索请求可组合标签、关键字、语义描述、AI 状态、宽高、文件大小、排序与随机种子。响应统一为：

```ts
interface SearchResult<T> {
  content: T[]
  page: number
  size: number
  hasNext: boolean
}
```

前端只呈现上一页/下一页，不依赖精确总数。图库卡片优先加载 `thumbnailUrl`；缩略图尚未 backfill 或返回 404 时，图片元素回退到 `imageUrl`。

## 上传交互

```mermaid
sequenceDiagram
    actor U as 用户
    participant V as UploadView
    participant Q as 前端并发队列
    participant W as Web Service

    U->>V: 拖拽或批量选择文件
    V->>Q: 添加待上传文件
    loop 受控并发
        Q->>W: POST /api/upload
        W-->>Q: 已创建后端任务
    end
    V->>W: 轮询 GET /api/upload/tasks
    W-->>V: pendingCount / processingTask / failedTasks
    U->>V: 重试或清空失败项
    V->>W: POST 或 DELETE /api/upload/tasks
```

浏览器端上传完成只表示后端已将文件写入 MinIO staging 并创建 PostgreSQL 任务，不表示原图归档或 AI 处理已经完成。页面通过任务轮询展示后端入库阶段的进度。

## AI 状态展示

- `PENDING`：等待首次处理或自动退避重试。
- `PROCESSING`：Web Service Worker 已领取持久化 AI Job。
- `READY`：标签与图像向量已经持久化。
- `FAILED`：自动尝试已耗尽；详情显示 `aiError` 并允许手动重试。

图库侧栏可以按四种状态过滤。`PENDING` 任务由后台 Worker 自动处理，不需要前端批量入队。

## 容器构建

Frontend Dockerfile 在 Node 构建阶段执行 `pnpm install --frozen-lockfile` 和 `pnpm build`，再将静态产物复制到 Nginx 运行镜像。浏览器的 `/api/*` 与 `/oss/*` 请求统一由容器内 Nginx 转发，不提供 Vite 宿主机代理配置。
