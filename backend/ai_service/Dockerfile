# 1. 使用包含 CUDA 运行时的基础镜像（Ubuntu 22.04）
FROM nvidia/cuda:12.4.1-cudnn-runtime-ubuntu22.04

# 2. 设置环境变量：防止生成 .pyc 文件，确保日志实时输出
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    DEBIAN_FRONTEND=noninteractive \
    PIP_DEFAULT_TIMEOUT=300 \
    PIP_RETRIES=10

WORKDIR /app

# 3. 安装 Python 与 pip
RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip python3-venv ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# 4. 安装 Python 依赖
COPY requirements.txt .
RUN python3 -m pip install --upgrade pip setuptools wheel && \
    python3 -m pip install -r requirements.txt

# 5. 复制源代码
COPY . .

# 6. 启动命令
# 注意：host 必须是 0.0.0.0，否则外部 (Java) 连不进来
CMD ["python3", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0"]
