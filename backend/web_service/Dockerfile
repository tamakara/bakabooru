# ==========================================
# Stage 1: 编译阶段 (Maven + Java 21)
# ==========================================
# 使用官方 Maven 镜像，基于 Eclipse Temurin JDK 21
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 1. 预下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. 复制源码并打包
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: 运行阶段 (JRE 21 Alpine)
# ==========================================
# 使用极简的 Alpine 版 JRE 21，体积小且安全
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 从 Builder 阶段复制 jar 包
# 注意：这里假设你的 jar 包在 target 目录下
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令 (Java 21 默认开启了 ZGC 等优化，无需额外参数)
ENTRYPOINT ["java", "-jar", "app.jar"]