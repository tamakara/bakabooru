# syntax=docker/dockerfile:1

# ==========================================
# Stage 1: 编译阶段 (Maven + Java 21)
# ==========================================
# 使用官方 Maven 镜像，基于 Eclipse Temurin JDK 21
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

RUN mkdir -p /root/.m2 && \
    printf '%s\n' \
      '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"' \
      '          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' \
      '          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings.xsd">' \
      '  <profiles>' \
      '    <profile>' \
      '      <id>multi-repo</id>' \
      '      <repositories>' \
      '        <repository>' \
      '          <id>tencent</id>' \
      '          <url>https://mirrors.cloud.tencent.com/nexus/repository/maven-public/</url>' \
      '          <releases><enabled>true</enabled></releases>' \
      '          <snapshots><enabled>false</enabled></snapshots>' \
      '        </repository>' \
      '        <repository>' \
      '          <id>huawei</id>' \
      '          <url>https://repo.huaweicloud.com/repository/maven/</url>' \
      '          <releases><enabled>true</enabled></releases>' \
      '          <snapshots><enabled>false</enabled></snapshots>' \
      '        </repository>' \
      '        <repository>' \
      '          <id>maven-central</id>' \
      '          <url>https://repo.maven.apache.org/maven2</url>' \
      '          <releases><enabled>true</enabled></releases>' \
      '          <snapshots><enabled>false</enabled></snapshots>' \
      '        </repository>' \
      '      </repositories>' \
      '      <pluginRepositories>' \
      '        <pluginRepository>' \
      '          <id>tencent</id>' \
      '          <url>https://mirrors.cloud.tencent.com/nexus/repository/maven-public/</url>' \
      '          <releases><enabled>true</enabled></releases>' \
      '          <snapshots><enabled>false</enabled></snapshots>' \
      '        </pluginRepository>' \
      '        <pluginRepository>' \
      '          <id>huawei</id>' \
      '          <url>https://repo.huaweicloud.com/repository/maven/</url>' \
      '          <releases><enabled>true</enabled></releases>' \
      '          <snapshots><enabled>false</enabled></snapshots>' \
      '        </pluginRepository>' \
      '        <pluginRepository>' \
      '          <id>maven-central</id>' \
      '          <url>https://repo.maven.apache.org/maven2</url>' \
      '          <releases><enabled>true</enabled></releases>' \
      '          <snapshots><enabled>false</enabled></snapshots>' \
      '        </pluginRepository>' \
      '      </pluginRepositories>' \
      '    </profile>' \
      '  </profiles>' \
      '  <activeProfiles>' \
      '    <activeProfile>multi-repo</activeProfile>' \
      '  </activeProfiles>' \
      '</settings>' > /root/.m2/settings.xml

# 1. 复制源码并打包
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository mvn clean package -DskipTests

# ==========================================
# Stage 2: 运行阶段 (JRE 21 Alpine)
# ==========================================
# 使用极简的 Alpine 版 JRE 21，体积小且安全
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 从 Builder 阶段复制 jar 包
# 注意：这里假设你的 jar 包在 target 目录下
COPY --from=builder /app/target/*.jar app.jar

# 启动命令 (Java 21 默认开启了 ZGC 等优化，无需额外参数)
ENTRYPOINT ["java", "-jar", "app.jar"]
