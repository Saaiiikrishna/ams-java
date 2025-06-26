# Dockerfile for Shared Library
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build and install to local repository
RUN mvn clean install -DskipTests

# Final stage - just for building, no runtime needed
FROM alpine:latest
RUN echo "Shared library built successfully"
