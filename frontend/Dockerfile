# Stage 1: Build (Jenkins sırasında)
FROM node:18-alpine AS build
WORKDIR /app

COPY package*.json ./

# Jenkins'deki NODE_OPTIONS ile uyumlu (256MB)
ENV NODE_OPTIONS="--max-old-space-size=512"
RUN npm ci --legacy-peer-deps --silent --prefer-offline --no-audit

COPY . .
RUN npm run build && rm -rf node_modules && npm cache clean --force  ✅
# Stage 2: Runtime (Nginx)
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]