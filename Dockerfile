# Multi-stage Dockerfile for Attendance Management System Backend
# Optimized for production deployment with minimal image size

# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven configuration files first (for better layer caching)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
COPY mvnw.cmd .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install necessary packages for production
RUN apk add --no-cache \
    curl \
    dumb-init \
    tzdata \
    && rm -rf /var/cache/apk/*

# Set timezone
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create directories for logs and uploads
RUN mkdir -p logs uploads/faces uploads/profiles && \
    chown -R appuser:appgroup /app

# Copy application configuration
COPY --chown=appuser:appgroup src/main/resources/application.properties application.properties

# Health check script
COPY --chown=appuser:appgroup <<EOF /app/healthcheck.sh
#!/bin/sh
curl -f http://localhost:8080/actuator/health || exit 1
EOF

RUN chmod +x /app/healthcheck.sh

# Switch to non-root user
USER appuser

# Expose ports
EXPOSE 8080 9090

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=docker \
    LOGGING_LEVEL_ROOT=INFO \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels for metadata
LABEL maintainer="Attendance Management System" \
      version="1.0.0" \
      description="Attendance Management System Backend with mDNS Service Discovery" \
      org.opencontainers.image.title="Attendance Management System Backend" \
      org.opencontainers.image.description="Spring Boot backend with gRPC, mDNS discovery, and observability" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.vendor="Attendance Management System"
