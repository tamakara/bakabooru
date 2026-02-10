import time
from typing import List, Optional, Tuple

from langchain_huggingface import HuggingFaceEmbeddings
from langchain_core.documents import Document

from app.core.config import config
from app.core.vector_store import PostgresVectorStore

# 常量配置
MODEL_REPO = "sentence-transformers/all-MiniLM-L6-v2"
COLLECTION_NAME = "tag_vectors"


class TagMatcher:
    def __init__(self, device: str = "cpu"):
        # 1. 初始化嵌入模型
        print(f"正在加载嵌入模型: {MODEL_REPO}...")
        self.embeddings = HuggingFaceEmbeddings(
            model_name=MODEL_REPO,
            model_kwargs={'device': device},
            encode_kwargs={'normalize_embeddings': True},
            cache_folder=str(config.MODEL_CACHE_DIR),
        )

        # 2. 初始化向量存储包装器
        self.vector_db = PostgresVectorStore(
            embeddings=self.embeddings,
            collection_name=COLLECTION_NAME
        )
        print("PGVector 连接已初始化")

    def rebuild_index(self, valid_tags: List[str]):
        """
        全量重建索引：清空旧数据 -> 插入新数据
        """
        if not valid_tags:
            print("警告: 提供的标签列表为空，跳过重建。")
            return

        print(f"正在重建 PostgreSQL 向量索引 (共 {len(valid_tags)} 个标签)...")
        start_time = time.time()

        # 1. 预处理 Tag -> Document
        documents = [
            Document(
                # page_content 是用于搜索的文本 (语义)
                page_content=tag.replace("_", " ").replace("(", "").replace(")", ""),
                # metadata 存储原始标签，方便取回
                metadata={"original_tag": tag}
            ) for tag in valid_tags
        ]

        # 2. 清空旧数据 (防止重复)
        self.vector_db.clear_collection()
        print("旧索引已清理。")

        # 3. 批量插入新数据
        self.vector_db.add_documents(documents)

        print(f"索引已写入数据库，耗时: {time.time() - start_time:.2f}s")

    def match(self, query: str, threshold: float = 0.6) -> Optional[Tuple[str, float]]:
        """
        在数据库中执行向量相似度搜索
        """
        clean_q = query.replace("_", " ").replace("(", "").replace(")", "").lower()

        # 执行搜索
        search_res = self.vector_db.search(clean_q, k=1)

        if search_res:
            doc, distance = search_res[0]

            # 假设 LangChain 默认行为 (Cosine Distance), similarity = 1 - distance
            similarity = 1 - distance

            if similarity >= threshold:
                return doc.metadata["original_tag"], float(similarity)

        return None
