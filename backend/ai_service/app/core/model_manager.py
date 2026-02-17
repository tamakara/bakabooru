"""模型管理模块 - 负责加载和管理所有AI模型"""
from typing import Optional

import numpy as np
import onnxruntime as ort
from transformers import CLIPProcessor
from langchain_community.embeddings import FastEmbedEmbeddings
from huggingface_hub import hf_hub_download

from app.core.settings import settings


# HuggingFace CLIP 模型名称
CLIP_MODEL_NAME = "openai/clip-vit-base-patch32"
# ONNX 版本的 CLIP 模型
CLIP_ONNX_REPO = "Xenova/clip-vit-base-patch32"


class ModelManager:
    """单例模式的模型管理器，负责加载和缓存所有模型"""

    _instance: Optional["ModelManager"] = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if self._initialized:
            return
        self._initialized = True

        self._clip_text_session = None
        self._clip_vision_session = None
        self._clip_processor = None
        self._embeddings = None
        self._camie_tagger = None
        self._device = settings.DEVICE
        self._ort_providers = self._get_ort_providers()

    @property
    def device(self) -> str:
        return self._device

    def _get_ort_providers(self) -> list:
        """获取 ONNX Runtime 的执行提供器列表"""
        providers = []
        if self._device == "cuda":
            # 检查 CUDA provider 是否可用
            available = ort.get_available_providers()
            if "CUDAExecutionProvider" in available:
                providers.append("CUDAExecutionProvider")
            else:
                print("警告: CUDA 不可用，回退到 CPU")
        providers.append("CPUExecutionProvider")
        return providers

    @property
    def clip_text_session(self) -> ort.InferenceSession:
        """懒加载 CLIP 文本编码器 ONNX session"""
        if self._clip_text_session is None:
            self._load_clip()
        return self._clip_text_session

    @property
    def clip_vision_session(self) -> ort.InferenceSession:
        """懒加载 CLIP 图像编码器 ONNX session"""
        if self._clip_vision_session is None:
            self._load_clip()
        return self._clip_vision_session

    @property
    def clip_processor(self) -> CLIPProcessor:
        """懒加载 CLIP 处理器"""
        if self._clip_processor is None:
            self._load_clip()
        return self._clip_processor

    @property
    def embeddings(self) -> FastEmbedEmbeddings:
        """懒加载文本嵌入模型"""
        if self._embeddings is None:
            self._load_embeddings()
        return self._embeddings

    @property
    def camie_tagger(self):
        """懒加载 CamieTagger"""
        if self._camie_tagger is None:
            self._load_camie_tagger()
        return self._camie_tagger

    def _load_clip(self):
        """加载 CLIP 模型（使用 ONNX Runtime 加速）"""
        print(f"正在加载 CLIP ONNX 模型: {CLIP_ONNX_REPO}...")
        cache_dir = str(settings.MODEL_CACHE_DIR)

        # 下载 ONNX 模型文件
        text_model_path = hf_hub_download(
            repo_id=CLIP_ONNX_REPO,
            filename="onnx/text_model.onnx",
            cache_dir=cache_dir
        )
        vision_model_path = hf_hub_download(
            repo_id=CLIP_ONNX_REPO,
            filename="onnx/vision_model.onnx",
            cache_dir=cache_dir
        )

        # 创建 ONNX 会话
        sess_options = ort.SessionOptions()
        sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL

        self._clip_text_session = ort.InferenceSession(
            text_model_path,
            sess_options=sess_options,
            providers=self._ort_providers
        )
        self._clip_vision_session = ort.InferenceSession(
            vision_model_path,
            sess_options=sess_options,
            providers=self._ort_providers
        )

        # 加载处理器（用于预处理）
        self._clip_processor = CLIPProcessor.from_pretrained(
            CLIP_MODEL_NAME,
            cache_dir=cache_dir,
            use_fast=True
        )

        active_provider = self._clip_text_session.get_providers()[0]
        print(f"CLIP ONNX 模型加载完成，使用: {active_provider}")

    def _load_embeddings(self):
        """加载文本嵌入模型"""
        print("正在加载文本嵌入模型...")
        self._embeddings = FastEmbedEmbeddings(
            model_name="sentence-transformers/all-MiniLM-L6-v2",
            cache_dir=str(settings.MODEL_CACHE_DIR),
        )
        print("文本嵌入模型加载完成")

    def _load_camie_tagger(self):
        """加载 CamieTagger"""
        from app.models.camie_tagger import CamieTagger
        print("正在加载 CamieTagger...")
        self._camie_tagger = CamieTagger(
            device=self._device,
            cache_dir=settings.MODEL_CACHE_DIR
        )
        print("CamieTagger 加载完成")

    def encode_text_clip(self, text: str) -> np.ndarray:
        """使用 CLIP 编码文本，返回归一化的特征向量"""
        inputs = self.clip_processor(text=[text], return_tensors="np", padding=True)

        # 转换为 numpy 用于 ONNX 推理
        ort_inputs = {
            "input_ids": inputs["input_ids"].astype(np.int64),
#             "attention_mask": inputs["attention_mask"].astype(np.int64),
        }
        outputs = self.clip_text_session.run(None, ort_inputs)
        text_embeds = outputs[0]  # text_embeds

        # 归一化
        text_embeds = text_embeds / np.linalg.norm(text_embeds, axis=-1, keepdims=True)
        return text_embeds

    def encode_image_clip(self, image) -> np.ndarray:
        """使用 CLIP 编码图像，返回归一化的特征向量"""
        inputs = self.clip_processor(images=image, return_tensors="np")

        # 转换为 numpy 用于 ONNX 推理
        ort_inputs = {
            "pixel_values": inputs["pixel_values"].astype(np.float32),
        }
        outputs = self.clip_vision_session.run(None, ort_inputs)
        image_embeds = outputs[0]  # image_embeds

        # 归一化
        image_embeds = image_embeds / np.linalg.norm(image_embeds, axis=-1, keepdims=True)
        return image_embeds

    def preload_all(self):
        """预加载所有模型（用于服务启动时）"""
        print("预加载所有模型...")
        _ = self.embeddings
        _ = self.camie_tagger
        _ = self.clip_text_session  # 这会同时加载 text 和 vision session
        print("所有模型预加载完成")


# 全局单例
model_manager = ModelManager()
