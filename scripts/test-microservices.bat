@echo off
REM Test script for microservices deployment
REM This script tests all deployed microservices to ensure functionality

echo ========================================
echo Microservices Testing Script
echo ========================================
echo.

REM Function to test HTTP endpoint
:test_endpoint
set URL=%1
set SERVICE_NAME=%2
echo Testing %SERVICE_NAME%...
curl -s -o nul -w "%%{http_code}" %URL% > temp_response.txt
set /p HTTP_CODE=<temp_response.txt
del temp_response.txt

if "%HTTP_CODE%"=="200" (
    echo ✓ %SERVICE_NAME% is healthy (HTTP %HTTP_CODE%)
) else (
    echo ✗ %SERVICE_NAME% failed (HTTP %HTTP_CODE%)
)
goto :eof

REM Wait for services to be ready
echo Waiting for services to start...
timeout /t 30 /nobreak >nul

echo.
echo Testing Infrastructure Services...
echo ================================

REM Test PostgreSQL
echo Testing PostgreSQL connection...
docker exec ams-postgres-microservices pg_isready -U postgres -d attendance_db >nul 2>&1
if errorlevel 1 (
    echo ✗ PostgreSQL is not ready
) else (
    echo ✓ PostgreSQL is healthy
)

echo.
echo Testing Microservices...
echo =======================

REM Test Auth Service
call :test_endpoint "http://localhost:8081/auth/actuator/health" "Auth Service"

REM Test Organization Service  
call :test_endpoint "http://localhost:8082/organization/actuator/health" "Organization Service"

REM Test Menu Service
call :test_endpoint "http://localhost:8085/menu/actuator/health" "Menu Service"

REM Test API Gateway
call :test_endpoint "http://localhost:8080/actuator/health" "API Gateway"

echo.
echo Testing Service Endpoints...
echo ===========================

REM Test Auth Service Test Endpoint
echo Testing Auth Service test endpoint...
curl -s "http://localhost:8081/auth/test/health" > temp_auth.txt 2>nul
if errorlevel 1 (
    echo ✗ Auth Service test endpoint failed
) else (
    echo ✓ Auth Service test endpoint working
)
del temp_auth.txt 2>nul

REM Test Organization Service Test Endpoint
echo Testing Organization Service test endpoint...
curl -s "http://localhost:8082/organization/test/health" > temp_org.txt 2>nul
if errorlevel 1 (
    echo ✗ Organization Service test endpoint failed
) else (
    echo ✓ Organization Service test endpoint working
)
del temp_org.txt 2>nul

echo.
echo Testing gRPC Services...
echo =======================

REM Test gRPC ports (basic connectivity)
echo Testing gRPC port connectivity...
netstat -an | findstr ":9091" >nul
if errorlevel 1 (
    echo ✗ Auth Service gRPC port (9091) not listening
) else (
    echo ✓ Auth Service gRPC port (9091) is listening
)

netstat -an | findstr ":9092" >nul
if errorlevel 1 (
    echo ✗ Organization Service gRPC port (9092) not listening
) else (
    echo ✓ Organization Service gRPC port (9092) is listening
)

echo.
echo Testing Monitoring Services...
echo =============================

REM Test Prometheus
call :test_endpoint "http://localhost:9090/-/healthy" "Prometheus"

REM Test Grafana
call :test_endpoint "http://localhost:3000/api/health" "Grafana"

echo.
echo Testing API Gateway Routing...
echo =============================

REM Test API Gateway routing to services
echo Testing API Gateway routing to Auth Service...
curl -s "http://localhost:8080/api/auth/actuator/health" > temp_gateway_auth.txt 2>nul
if errorlevel 1 (
    echo ✗ API Gateway -> Auth Service routing failed
) else (
    echo ✓ API Gateway -> Auth Service routing working
)
del temp_gateway_auth.txt 2>nul

echo Testing API Gateway routing to Organization Service...
curl -s "http://localhost:8080/api/organization/actuator/health" > temp_gateway_org.txt 2>nul
if errorlevel 1 (
    echo ✗ API Gateway -> Organization Service routing failed
) else (
    echo ✓ API Gateway -> Organization Service routing working
)
del temp_gateway_org.txt 2>nul

echo.
echo Container Status...
echo ==================
docker-compose -f docker-compose.microservices-simple.yml ps

echo.
echo Service Logs (Last 10 lines)...
echo ===============================
echo Auth Service logs:
docker-compose -f docker-compose.microservices-simple.yml logs --tail=5 auth-service

echo.
echo Organization Service logs:
docker-compose -f docker-compose.microservices-simple.yml logs --tail=5 organization-service

echo.
echo ========================================
echo Testing Complete!
echo ========================================
echo.
echo Service URLs:
echo - Auth Service: http://localhost:8081/auth/actuator/health
echo - Organization Service: http://localhost:8082/organization/actuator/health  
echo - Menu Service: http://localhost:8085/menu/actuator/health
echo - API Gateway: http://localhost:8080/actuator/health
echo - Prometheus: http://localhost:9090
echo - Grafana: http://localhost:3000 (admin/admin123)
echo.
echo API Gateway Routes:
echo - Auth API: http://localhost:8080/api/auth/
echo - Organization API: http://localhost:8080/api/organization/
echo - Menu API: http://localhost:8080/api/menu/
echo.
echo To stop services: docker-compose -f docker-compose.microservices-simple.yml down
echo To view logs: docker-compose -f docker-compose.microservices-simple.yml logs -f [service-name]
