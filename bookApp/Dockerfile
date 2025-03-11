# 构建阶段
FROM node:20.16.0-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制package.json和yarn.lock
COPY package.json yarn.lock ./

# 安装依赖
RUN yarn install

# 复制源代码
COPY . .

# 构建应用
RUN yarn build

# 运行阶段
FROM nginx:alpine

# 复制nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 添加环境变量替换脚本
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# 设置环境变量的默认值
ENV VITE_API_URL=http://localhost:9090
ENV VITE_ES_URL=http://elasticsearch:9200
ENV VITE_ES_USERNAME=
ENV VITE_ES_PASSWORD=

# 暴露端口
EXPOSE 80

# 使用自定义入口点脚本
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"] 