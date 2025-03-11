# 构建阶段
FROM maven:3.8.4-openjdk-17-slim AS builder

# 设置工作目录
WORKDIR /build

# 首先只复制pom.xml
COPY pom.xml .

# 下载依赖（使用.m2缓存提高构建速度）
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# 复制源代码和资源文件
COPY src ./src

# # 构建应用（使用.m2缓存提高构建速度）
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

# 运行阶段
FROM openjdk:17-slim

# 设置工作目录
WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制构建好的jar包
COPY --from=builder /build/target/*.jar app.jar

# 复制打包完成后的jar文件到/app目录下
# COPY target/*.jar /app/app.jar

# 创建配置目录
RUN mkdir -p /app/config /app/filedata

# 设置环境变量
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms1024m -Xmx2048m -XX:MaxRAMPercentage=75.0"

# 暴露端口
EXPOSE 9090

# 设置启动命令
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar /app/app.jar"] 