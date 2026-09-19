# 构建阶段: Node 20 构建前端产物
FROM node:20-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# 容器内前端通过 Nginx 同源访问 /api, 由 Nginx 反代到后端(见 nginx.conf)
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# 运行阶段: Nginx 托管静态产物并反代 API
FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
