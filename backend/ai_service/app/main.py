"""BaKaBooru AI Service 主入口"""
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api import tag_image_router, semantic_search_router, init_tags_router, image_embedding_router
from app.core.model_manager import model_manager


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理 - 启动时预加载模型"""
    print("AI Service 启动中，预加载模型...")
    model_manager.preload_all()
    print("AI Service 启动完成")
    yield
    print("AI Service 关闭")


app = FastAPI(
    title="BaKaBooru AI Service",
    description="图像打标、语义搜索等 AI 功能服务",
    version="2.0.0",
    lifespan=lifespan
)

# 注册路由
app.include_router(tag_image_router)
app.include_router(semantic_search_router)
app.include_router(init_tags_router)
app.include_router(image_embedding_router)


@app.get("/health")
def health():
    """健康检查接口"""
    return {"status": "ok"}
