# 项目结构

## 顶层目录

```text
bakabooru/
├── backend/               # Spring Boot API、业务模块和数据库迁移
├── model-manager/         # FastAPI 推理服务
├── frontend/              # Vue 3 + TypeScript 前端
├── docs/                  # 项目文档
├── data/                  # Compose 持久化目录
└── docker-compose.yml
```

## Backend

```text
backend/src/main/java/com/tamakara/bakabooru/
├── controller/
│   ├── ModelManagerController.java   # 模型目录和模型工件操作
│   ├── ImageController.java          # 图片 CRUD 和文件操作
│   ├── TagController.java            # 标签查询接口
│   └── ImageAnalysisController.java  # 图片分析和重试接口
├── module/image/                     # 图片、标签实体及其仓储、Mapper 和服务
├── module/analysis/                  # 分析 DTO、AnalysisJob、Worker 和任务服务
├── module/model/                     # Model、ModelManagerClient 和向量服务
├── module/search/                    # 搜索编排和搜索 DTO
├── module/upload/                    # 上传任务和入库 Worker
├── module/setting/                   # 设置实体、仓储、校验与 Backend 内部 Redis 缓存
├── module/auth/                      # 密码生命周期、登录和 JWT 认证
├── config/                           # Spring 配置、属性和拦截器
└── initializer/                      # MinIO bucket 和缩略图初始化
```

## 模块边界

- Controller 只负责 HTTP 参数、响应和状态码；业务流程放在对应模块服务中。
- `module.image` 不负责模型分析；分析任务通过 `module.analysis` 入队。
- `module.analysis` 可以依赖图片实体和 Model Manager 客户端，但图片 CRUD 不依赖分析服务。
- `module.model` 负责模型目录、远程推理客户端和向量生成，不负责图片任务状态。
- 外部 API 路径、数据库表和 JSON 字段由接口与迁移共同约束，重命名时需要兼容性评估。

## 本地验证

```bash
cd backend
mvn clean compile

cd ../frontend
pnpm install --frozen-lockfile
pnpm exec vue-tsc -b
pnpm run build
```

数据库和 Model Manager 依赖 Compose 环境；完整应用上下文测试需要 PostgreSQL 和 MinIO 已启动。
