# Multi-stage Dockerfile for attendance-service
# Optimized for production deployment

# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /app

# Install additional packages needed for protobuf compilation
RUN apk add --no-cache bash

# Copy service
WORKDIR /app/attendance-service
COPY attendance-service/pom.xml .
COPY attendance-service/.mvn/ .mvn/
COPY attendance-service/mvnw .
COPY attendance-service/mvnw.cmd .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY attendance-service/src/ src/

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install necessary packages
RUN apk add --no-cache curl dumb-init tzdata

# Set timezone
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime

# Create non-root user
RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/attendance-service/target/*.jar app.jar

# Create logs directory
RUN mkdir -p logs && chown -R appuser:appgroup /app

# Health check script
COPY --chown=appuser:appgroup <<EOF /app/healthcheck.sh
#!/bin/sh
curl -f http://localhost:8084/attendance/actuator/health || exit 1
EOF

RUN chmod +x /app/healthcheck.sh

# Switch to non-root user
USER appuser

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE=docker

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh

# Use dumb-init
ENTRYPOINT ["dumb-init", "--"]

# Start the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels
LABEL maintainer="Attendance Management System" \
      version="1.0.0" \
      description="attendance-service Microservice"
