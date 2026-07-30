# ==========================================
# Stage 1: 构建阶段 (Node.js)
# ==========================================
# 使用 Node 24 LTS Alpine
FROM node:24-alpine AS builder

# 1. 启用 Corepack
RUN corepack enable

WORKDIR /app

# 2. 复制依赖定义
COPY package.json pnpm-lock.yaml ./

# 3. 安装依赖 (建议使用淘宝源加速)
#RUN pnpm config set registry https://registry.npmmirror.com
RUN pnpm install --frozen-lockfile

# 4. 构建项目
COPY . .
RUN pnpm run build

# ==========================================
# Stage 2: 运行阶段 (Nginx)
# ==========================================
FROM nginx:alpine

# Let the official entrypoint discover the container runtime's DNS servers.
# Docker Compose resolves to 127.0.0.11; Kubernetes resolves to CoreDNS.
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/templates/default.conf.template

CMD ["nginx", "-g", "daemon off;"]
