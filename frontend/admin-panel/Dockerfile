# Multi-stage Dockerfile for Admin Panel (React)
# Optimized for production deployment with Nginx

# Stage 1: Build stage
FROM node:18-alpine AS builder

# Accept build argument for API base URL
ARG REACT_APP_API_BASE_URL=http://localhost:8080

# Set working directory
WORKDIR /app

# Copy package files first (for better layer caching)
COPY package*.json ./

# Install dependencies (including dev dependencies for build)
RUN npm ci --legacy-peer-deps --silent

# Copy source code
COPY . .

# Set environment variable for build
ENV REACT_APP_API_BASE_URL=${REACT_APP_API_BASE_URL}

# Build the application
RUN npm run build

# Stage 2: Production stage with Nginx
FROM nginx:alpine AS production

# Install curl for health checks
RUN apk add --no-cache curl

# Remove default nginx website
RUN rm -rf /usr/share/nginx/html/*

# Copy built application from builder stage
COPY --from=builder /app/build /usr/share/nginx/html

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Set permissions for existing nginx user
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d

# Create nginx PID directory
RUN mkdir -p /var/run/nginx && \
    chown -R nginx:nginx /var/run/nginx

# Switch to non-root user
USER nginx

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:80 || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]

# Labels for metadata
LABEL maintainer="Attendance Management System" \
      version="1.0.0" \
      description="Admin Panel for Attendance Management System" \
      org.opencontainers.image.title="AMS Admin Panel" \
      org.opencontainers.image.description="React-based admin panel with dynamic API discovery" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.vendor="Attendance Management System"
