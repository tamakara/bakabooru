# 1.使用 Python 3.12 Slim
FROM python:3.12-slim

# 2. 设置环境变量：防止生成 .pyc 文件，确保日志实时输出
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# 3. 安装 Python 依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 4. 复制源代码
COPY . .

# 5. 启动命令
# 注意：host 必须是 0.0.0.0，否则外部 (Java) 连不进来
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0"]
