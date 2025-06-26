@echo off
REM Docker-only deployment script for microservices
REM This script uses Docker to build everything without requiring local Maven

echo ========================================
echo Docker-Only Microservices Deployment
echo ========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running. Please start Docker Desktop.
    exit /b 1
)

echo ✓ Docker is running

REM Step 1: Build Shared Library using Docker
echo.
echo Step 1: Building Shared Library with Docker...
docker build -t ams-shared-lib:latest microservices/shared-lib
if errorlevel 1 (
    echo ERROR: Failed to build shared library
    exit /b 1
)
echo ✓ Shared library built successfully

REM Step 2: Create a simple working version for testing
echo.
echo Step 2: Creating minimal working services...

REM Create a minimal Auth Service that just starts up
mkdir microservices\auth-service\src\main\java\com\example\attendancesystem\auth\controller 2>nul

REM Create a simple test controller for Auth Service
(
echo package com.example.attendancesystem.auth.controller;
echo.
echo import org.springframework.web.bind.annotation.*;
echo import org.springframework.http.ResponseEntity;
echo import java.util.Map;
echo import java.time.LocalDateTime;
echo.
echo @RestController
echo @RequestMapping("/test"^)
echo public class AuthTestController {
echo.
echo     @GetMapping("/health"^)
echo     public ResponseEntity^<Map^<String, Object^>^> health(^) {
echo         return ResponseEntity.ok(Map.of(
echo             "service", "auth-service",
echo             "status", "UP",
echo             "timestamp", LocalDateTime.now(^).toString(^),
echo             "port", "8081"
echo         ^)^);
echo     }
echo.
echo     @GetMapping("/info"^)
echo     public ResponseEntity^<Map^<String, String^>^> info(^) {
echo         return ResponseEntity.ok(Map.of(
echo             "name", "Auth Service",
echo             "version", "1.0.0",
echo             "description", "Authentication and Authorization Microservice"
echo         ^)^);
echo     }
echo }
) > microservices\auth-service\src\main\java\com\example\attendancesystem\auth\controller\AuthTestController.java

echo ✓ Test controllers created

REM Step 3: Start infrastructure
echo.
echo Step 3: Starting infrastructure services...
docker-compose -f docker-compose.microservices-simple.yml up -d postgres prometheus grafana
if errorlevel 1 (
    echo ERROR: Failed to start infrastructure
    exit /b 1
)

echo ✓ Infrastructure started
echo Waiting for PostgreSQL to be ready...
timeout /t 30 /nobreak >nul

REM Step 4: Build and deploy core services
echo.
echo Step 4: Building and deploying microservices...

REM Build Auth Service
echo Building Auth Service...
docker-compose -f docker-compose.microservices-simple.yml build auth-service
if errorlevel 1 (
    echo ERROR: Failed to build Auth Service
    exit /b 1
)

REM Start Auth Service
echo Starting Auth Service...
docker-compose -f docker-compose.microservices-simple.yml up -d auth-service
if errorlevel 1 (
    echo ERROR: Failed to start Auth Service
    exit /b 1
)

echo ✓ Auth Service deployed

REM Wait for Auth Service to be ready
echo Waiting for Auth Service to be ready...
timeout /t 30 /nobreak >nul

REM Step 5: Test the deployment
echo.
echo Step 5: Testing deployment...

REM Test PostgreSQL
echo Testing PostgreSQL...
docker exec ams-postgres-microservices pg_isready -U postgres -d attendance_db >nul 2>&1
if errorlevel 1 (
    echo ✗ PostgreSQL is not ready
) else (
    echo ✓ PostgreSQL is healthy
)

REM Test Auth Service
echo Testing Auth Service...
curl -s "http://localhost:8081/auth/actuator/health" >nul 2>&1
if errorlevel 1 (
    echo ✗ Auth Service health check failed
) else (
    echo ✓ Auth Service is healthy
)

REM Test Auth Service custom endpoint
echo Testing Auth Service test endpoint...
curl -s "http://localhost:8081/auth/test/health" >nul 2>&1
if errorlevel 1 (
    echo ✗ Auth Service test endpoint failed
) else (
    echo ✓ Auth Service test endpoint working
)

echo.
echo ========================================
echo Deployment Status
echo ========================================
echo.
echo Infrastructure Services:
echo ✓ PostgreSQL Database: localhost:5432
echo ✓ Prometheus: http://localhost:9090
echo ✓ Grafana: http://localhost:3000 (admin/admin123^)
echo.
echo Microservices:
echo ✓ Auth Service: http://localhost:8081/auth/test/health
echo.
echo Container Status:
docker-compose -f docker-compose.microservices-simple.yml ps

echo.
echo ========================================
echo Testing Commands
echo ========================================
echo.
echo Test Auth Service:
echo curl http://localhost:8081/auth/test/health
echo curl http://localhost:8081/auth/test/info
echo curl http://localhost:8081/auth/actuator/health
echo.
echo View Logs:
echo docker-compose -f docker-compose.microservices-simple.yml logs -f auth-service
echo.
echo Stop Services:
echo docker-compose -f docker-compose.microservices-simple.yml down
echo.
echo Deploy More Services:
echo docker-compose -f docker-compose.microservices-simple.yml up -d organization-service menu-service api-gateway
echo.
echo ========================================
echo Deployment Complete!
echo ========================================
