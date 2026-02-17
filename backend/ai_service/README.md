# BaKaBooru AI Service

图像打标、语义搜索等 AI 功能服务。

## 项目结构

```
ai_service/
├── app/
│   ├── api/                    # API 路由
│   │   ├── __init__.py
│   │   ├── init_tags.py        # 初始化标签向量
│   │   ├── semantic_search.py  # 语义搜索解析 (新)
│   │   └── tag_image.py        # 图像打标
│   ├── core/                   # 核心配置
│   │   ├── __init__.py
│   │   ├── database.py         # 数据库连接
│   │   ├── model_manager.py    # 模型管理器 (单例)
│   │   └── settings.py         # 配置管理
│   ├── models/                 # AI 模型定义
│   │   ├── __init__.py
│   │   └── camie_tagger.py     # CamieTagger 打标器
│   ├── schemas/                # Pydantic 数据模型
│   │   ├── __init__.py
│   │   ├── common.py           # 通用响应模型
│   │   ├── semantic_search.py  # 语义搜索相关
│   │   └── tag_image.py        # 图像打标相关
│   ├── services/               # 业务逻辑
│   │   ├── __init__.py
│   │   ├── init_tags_service.py
│   │   ├── semantic_search_service.py
│   │   ├── tag_image_service.py
│   │   ├── tag_matcher_service.py
│   │   └── prompts/            # LLM Prompt 定义
│   │       ├── __init__.py
│   │       └── semantic_search_prompt.py
│   └── main.py                 # FastAPI 入口
├── requirements.txt
└── Dockerfile
```

## API 端点

### 图像打标
- `POST /tag/image` - 对图像进行自动打标

### 语义搜索
- `POST /search/semantic` - 将自然语言描述解析为搜索条件
  - 返回标签搜索条件 (硬性条件)
  - 返回 CLIP 向量 (软性条件，用于相似度搜索)

### 标签管理
- `POST /tags/init` - 为数据库中的标签生成向量

### 健康检查
- `GET /health` - 健康检查

## 模型

服务启动时会预加载以下模型：
1. **CamieTagger-V2** - 动漫风格图像打标
2. **CLIP ViT-B/32** (`openai/clip-vit-base-patch32`) - 图文对齐模型，用于语义搜索
3. **all-MiniLM-L6-v2** - 文本嵌入模型，用于标签匹配

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| MODEL_CACHE_DIR | 模型缓存目录 | /model_cache |
| DB_HOST | 数据库地址 | localhost |
| DB_PORT | 数据库端口 | 5432 |
| DB_USER | 数据库用户 | postgres |
| DB_PASS | 数据库密码 | password |
| DB_NAME | 数据库名 | bakabooru |
| DEVICE | 推理设备 | cuda |
