# Stage 1: Build React App
FROM node:18-alpine as build
WORKDIR /app

# Copy only package files first (better caching)
COPY package*.json ./

# Use npm ci instead of npm install (faster, deterministic)
RUN npm ci --cache /root/.npm --prefer-offline --silent

# Copy source code
COPY . .

# Build with memory limit
ENV NODE_OPTIONS="--max-old-space-size=1024"
RUN npm run build

# Stage 2: Serve via Nginx
FROM nginx:alpine

# Copy built files from build stage
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]