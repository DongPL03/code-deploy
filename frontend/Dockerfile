# --- Stage 1: Build Angular App ---
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --legacy-peer-deps

# Copy source code
COPY . .

# Build production (static files)
RUN npm run build -- --configuration production

# --- Stage 2: Serve with Nginx ---
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static files
RUN rm -rf ./*

# Copy built Angular app from Stage 1
# Angular 20 SSR output structure: dist/frontend/browser
COPY --from=build /app/dist/frontend/browser .

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
