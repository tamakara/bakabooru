# 开发指南

本文记录源码位置、本地检查入口和贡献约定，不替代部署步骤或接口说明。首次运行见[快速开始](getting-started.md)，用户功能见[功能与 API](features-api.md)。

## 代码位置

| 路径 | 内容 | 技术 |
| --- | --- | --- |
| `frontend/` | 页面、组件、客户端 API | Vue 3、TypeScript、Vite、Pinia、Vue Query |
| `backend/` | REST API、业务逻辑、迁移 | Java 21、Spring Boot、Flyway |
| `model-manager/` | 推理 API、模型生命周期 | Python 3.12、FastAPI、ONNX Runtime GPU |
| `docker-compose.yml` | 单机部署编排 | Docker Compose |

## 本地验证

Backend 测试：

```bash
cd backend
mvn test
```

前端构建：

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm run build
```

Model Manager 依赖 Python 3.12、Linux x86_64 和 CUDA；推荐在容器环境运行测试：

```bash
docker compose run --rm model-manager /app/.venv/bin/python -m pytest tests -q
```

## 变更约定

- 数据库变更追加 Flyway migration，不修改已执行过的 migration。
- API 变更同步更新[功能与 API](features-api.md)；数据所有权或请求流改变时更新[架构设计](architecture.md)。
- 新增部署变量或改变默认值时更新[部署说明](deployment.md)。
- 文档按主题独立维护，不复制其他页面的完整流程或配置表。
