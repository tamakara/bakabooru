"""数据库连接管理"""
import psycopg2
from pgvector.psycopg2 import register_vector
from contextlib import contextmanager

from app.core.settings import settings


@contextmanager
def get_db_connection():
    """获取数据库连接的上下文管理器"""
    conn = psycopg2.connect(
        host=settings.DB_HOST,
        port=settings.DB_PORT,
        user=settings.DB_USER,
        password=settings.DB_PASS,
        dbname=settings.DB_NAME
    )
    register_vector(conn)
    try:
        yield conn
    finally:
        conn.close()
