"""初始化标签向量服务"""
from app.core.database import get_db_connection
from app.core.model_manager import model_manager


class InitTagsService:
    """初始化标签向量服务"""

    def init_tags(self) -> dict:
        """
        为数据库中 embedding 为空的标签生成向量
        """
        try:
            with get_db_connection() as conn:
                cur = conn.cursor()

                # 获取所有 embedding 为空的标签
                cur.execute("SELECT id, name FROM tags WHERE embedding IS NULL")
                rows = cur.fetchall()

                if not rows:
                    return {"message": "No tags found with empty embeddings."}

                embeddings = model_manager.embeddings
                updates = 0

                for row in rows:
                    tag_id = row[0]
                    tag_name = row[1]

                    # 清理标签名称用于生成向量
                    clean_tag = tag_name.replace("_", " ").replace("(", "").replace(")", "")

                    # 生成向量
                    embedding_vector = embeddings.embed_query(clean_tag)

                    # 更新数据库
                    cur.execute(
                        "UPDATE tags SET embedding = %s WHERE id = %s",
                        (embedding_vector, tag_id)
                    )
                    updates += 1

                conn.commit()
                cur.close()
                return {"message": f"Successfully updated {updates} tags."}

        except Exception as e:
            return {"error": str(e)}


# 单例服务实例
init_tags_service = InitTagsService()
