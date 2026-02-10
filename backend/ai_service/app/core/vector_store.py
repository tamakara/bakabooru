from typing import List, Tuple

from langchain_postgres import PGVector
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings

from app.core.config import config


class PostgresVectorStore:
    def __init__(self, embeddings: Embeddings, collection_name: str):
        """
        初始化 PostgreSQL 向量存储 wrapper
        """
        # 构建数据库连接字符串
        self.connection_string = (
            f"postgresql+psycopg2://{config.DB_USER}:{config.DB_PASS}"
            f"@{config.DB_HOST}:{config.DB_PORT}/{config.DB_NAME}"
        )

        # 初始化向量存储 (PGVector)
        self.vector_store = PGVector(
            embeddings=embeddings,
            collection_name=collection_name,
            connection=self.connection_string,
            use_jsonb=True,
        )

    def clear_collection(self) -> None:
        """清空集合中的所有数据"""
        try:
            self.vector_store.delete_collection()
        except Exception as e:
            # 可能是首次运行，集合不存在
            print(f"清理索引时发生轻微异常 (可能是首次运行): {e}")

    def add_documents(self, documents: List[Document]) -> None:
        """批量添加文档"""
        self.vector_store.add_documents(documents)

    def search(self, query: str, k: int = 1) -> List[Tuple[Document, float]]:
        """
        执行相似度搜索
        返回: List[(Document, score)]
        """
        return self.vector_store.similarity_search_with_score(query, k=k)
