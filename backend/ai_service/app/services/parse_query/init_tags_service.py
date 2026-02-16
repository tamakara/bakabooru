import psycopg2
from pgvector.psycopg2 import register_vector
from langchain_community.embeddings import FastEmbedEmbeddings
from app.core.config import config


class InitTagsService:
    def __init__(self):
        self.model_repo = "sentence-transformers/all-MiniLM-L6-v2"
        # 初始化 embedding 模型
        self.embeddings = FastEmbedEmbeddings(
            model_name=self.model_repo,
            cache_dir=str(config.MODEL_CACHE_DIR),
        )

    def init_tags(self):
        try:
            conn = psycopg2.connect(
                host=config.DB_HOST,
                port=config.DB_PORT,
                user=config.DB_USER,
                password=config.DB_PASS,
                dbname=config.DB_NAME
            )

            # Register vector type
            register_vector(conn)

            cur = conn.cursor()

            # 获取所有 embedding 为空的标签
            cur.execute("SELECT id, name FROM tags WHERE embedding IS NULL")
            rows = cur.fetchall()

            if not rows:
                return {"message": "No tags found with empty embeddings."}

            updates = 0
            for row in rows:
                tag_id = row[0]
                tag_name = row[1]

                # 清理标签名称用于生成向量 (移除下划线等)
                clean_tag = tag_name.replace("_", " ").replace("(", "").replace(")", "")

                # 生成向量
                embedding_vector = self.embeddings.embed_query(clean_tag)

                # 更新数据库
                cur.execute(
                    "UPDATE tags SET embedding = %s WHERE id = %s",
                    (embedding_vector, tag_id)
                )
                updates += 1

            conn.commit()
            cur.close()
            conn.close()
            return {"message": f"Successfully updated {updates} tags."}

        except Exception as e:
            return {"error": str(e)}
