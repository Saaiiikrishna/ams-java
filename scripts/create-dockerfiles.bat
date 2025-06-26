@echo off
REM Script to create Dockerfiles for all microservices

echo Creating Dockerfiles for all microservices...

REM Define services
set SERVICES=organization-service subscriber-service attendance-service menu-service order-service table-service api-gateway

REM Create Dockerfile for each service
for %%s in (%SERVICES%) do (
    echo Creating Dockerfile for %%s...
    
    (
    echo # Multi-stage Dockerfile for %%s
    echo # Optimized for production deployment
    echo.
    echo # Stage 1: Build stage
    echo FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
    echo.
    echo # Set working directory
    echo WORKDIR /app
    echo.
    echo # Copy shared library first
    echo COPY ../shared-lib /app/shared-lib
    echo WORKDIR /app/shared-lib
    echo RUN mvn clean install -DskipTests
    echo.
    echo # Copy service
    echo WORKDIR /app/%%s
    echo COPY pom.xml .
    echo COPY .mvn/ .mvn/
    echo COPY mvnw .
    echo COPY mvnw.cmd .
    echo.
    echo # Download dependencies
    echo RUN mvn dependency:go-offline -B
    echo.
    echo # Copy source code
    echo COPY src/ src/
    echo.
    echo # Build the application
    echo RUN mvn clean package -DskipTests -B
    echo.
    echo # Stage 2: Runtime stage
    echo FROM eclipse-temurin:21-jre-alpine AS runtime
    echo.
    echo # Install necessary packages
    echo RUN apk add --no-cache curl dumb-init tzdata
    echo.
    echo # Set timezone
    echo ENV TZ=Asia/Kolkata
    echo RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime
    echo.
    echo # Create non-root user
    echo RUN addgroup -g 1001 -S appgroup ^&^& adduser -u 1001 -S appuser -G appgroup
    echo.
    echo # Set working directory
    echo WORKDIR /app
    echo.
    echo # Copy the built JAR
    echo COPY --from=builder /app/%%s/target/*.jar app.jar
    echo.
    echo # Create logs directory
    echo RUN mkdir -p logs ^&^& chown -R appuser:appgroup /app
    echo.
    echo # Health check script
    echo COPY --chown=appuser:appgroup ^<^<EOF /app/healthcheck.sh
    echo #!/bin/sh
    echo curl -f http://localhost:8080/actuator/health ^|^| exit 1
    echo EOF
    echo.
    echo RUN chmod +x /app/healthcheck.sh
    echo.
    echo # Switch to non-root user
    echo USER appuser
    echo.
    echo # Environment variables
    echo ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC" \
    echo     SPRING_PROFILES_ACTIVE=docker
    echo.
    echo # Health check
    echo HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    echo     CMD /app/healthcheck.sh
    echo.
    echo # Use dumb-init
    echo ENTRYPOINT ["dumb-init", "--"]
    echo.
    echo # Start the application
    echo CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
    echo.
    echo # Labels
    echo LABEL maintainer="Attendance Management System" \
    echo       version="1.0.0" \
    echo       description="%%s Microservice"
    ) > "microservices\%%s\Dockerfile"
)

echo.
echo Creating Shared Library Dockerfile...

(
echo # Dockerfile for Shared Library
echo FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
echo.
echo WORKDIR /app
echo.
echo # Copy pom.xml first for better caching
echo COPY pom.xml .
echo.
echo # Download dependencies
echo RUN mvn dependency:go-offline -B
echo.
echo # Copy source code
echo COPY src/ src/
echo.
echo # Build and install to local repository
echo RUN mvn clean install -DskipTests
echo.
echo # Final stage - just for building, no runtime needed
echo FROM alpine:latest
echo RUN echo "Shared library built successfully"
) > "microservices\shared-lib\Dockerfile"

echo.
echo All Dockerfiles created successfully!
echo.
echo Created Dockerfiles for:
for %%s in (%SERVICES%) do (
    echo - %%s
)
echo - shared-lib
echo.
echo Next steps:
echo 1. Update package imports in copied files
echo 2. Create application.yml for each service
echo 3. Test microservices build
echo 4. Deploy using docker-compose.microservices.yml
