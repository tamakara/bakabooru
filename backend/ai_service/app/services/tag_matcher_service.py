"""标签匹配服务 - 使用向量相似度匹配标签"""
from typing import Optional, Tuple

from app.core.database import get_db_connection
from app.core.model_manager import model_manager


class TagMatcherService:
    """标签匹配服务"""

    def match(self, query: str, threshold: float = 0.6) -> Optional[Tuple[str, float]]:
        """
        在数据库中执行向量相似度搜索
        :param query: 查询标签
        :param threshold: 相似度阈值
        :return: (标签名, 相似度) 或 None
        """
        # 清理查询文本
        clean_q = query.replace("_", " ").replace("(", "").replace(")", "").lower()
        query_embedding = model_manager.embeddings.embed_query(clean_q)

        with get_db_connection() as conn:
            cur = conn.cursor()
            try:
                # 计算余弦相似度 - 需要将数组转换为 vector 类型
                cur.execute(
                    """
                    SELECT name, 1 - (embedding <=> %s::vector) as similarity
                    FROM tags
                    WHERE 1 - (embedding <=> %s::vector) >= %s
                    ORDER BY similarity DESC
                    LIMIT 1
                    """,
                    (query_embedding, query_embedding, threshold)
                )
                row = cur.fetchone()
                if row:
                    return row[0], row[1]
                return None
            finally:
                cur.close()


# 单例服务实例
tag_matcher_service = TagMatcherService()
