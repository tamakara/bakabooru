import psycopg2
from typing import Optional, Tuple
from pgvector.psycopg2 import register_vector
from langchain_community.embeddings import FastEmbedEmbeddings

from app.core.config import config

# 常量配置
MODEL_REPO = "sentence-transformers/all-MiniLM-L6-v2"


class TagMatcher:
    def __init__(self):
        # 1. 初始化嵌入模型
        print(f"正在加载嵌入模型: {MODEL_REPO}...")
        self.embeddings = FastEmbedEmbeddings(
            model_name=MODEL_REPO,
            cache_dir=str(config.MODEL_CACHE_DIR),
        )
        print("嵌入模型已加载")

    def _get_conn(self):
        conn = psycopg2.connect(
            host=config.DB_HOST,
            port=config.DB_PORT,
            user=config.DB_USER,
            password=config.DB_PASS,
            dbname=config.DB_NAME
        )
        register_vector(conn)
        return conn

    def match(self, query: str, threshold: float = 0.6) -> Optional[Tuple[str, float]]:
        """
        在数据库中执行向量相似度搜索
        """
        clean_q = query.replace("_", " ").replace("(", "").replace(")", "").lower()
        query_embedding = self.embeddings.embed_query(clean_q) # Returns List[float]

        conn = self._get_conn()
        cur = conn.cursor()

        try:
            # Calculate cosine distance: <=>
            # similarity = 1 - distance
            # We want similarity >= threshold  =>  1 - distance >= threshold  =>  distance <= 1 - threshold

            cur.execute(
                "SELECT name, 1 - (embedding <=> %s) as similarity FROM tags WHERE 1 - (embedding <=> %s) >= %s ORDER BY similarity DESC LIMIT 1",
                (query_embedding, query_embedding, threshold)
            )
            row = cur.fetchone()

            if row:
                return row[0], row[1]
            return None
        finally:
            cur.close()
            conn.close()

