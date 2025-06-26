@echo off
REM Script to create complete microservices structure
REM This script creates all necessary directories and basic files for each microservice

echo Creating complete microservices structure...

REM Define services
set SERVICES=subscriber-service attendance-service menu-service order-service table-service api-gateway

REM Create directory structure for each service
for %%s in (%SERVICES%) do (
    echo Creating structure for %%s...
    
    REM Create main directories
    mkdir microservices\%%s\src\main\java\com\example\attendancesystem\%%~ns 2>nul
    mkdir microservices\%%s\src\main\java\com\example\attendancesystem\%%~ns\model 2>nul
    mkdir microservices\%%s\src\main\java\com\example\attendancesystem\%%~ns\repository 2>nul
    mkdir microservices\%%s\src\main\java\com\example\attendancesystem\%%~ns\service 2>nul
    mkdir microservices\%%s\src\main\java\com\example\attendancesystem\%%~ns\controller 2>nul
    mkdir microservices\%%s\src\main\java\com\example\attendancesystem\%%~ns\config 2>nul
    mkdir microservices\%%s\src\main\java\com\example\attendancesystem\%%~ns\grpc 2>nul
    mkdir microservices\%%s\src\main\resources 2>nul
    mkdir microservices\%%s\src\test\java 2>nul
)

REM Create specific directories for API Gateway
mkdir microservices\api-gateway\src\main\java\com\example\attendancesystem\gateway\filter 2>nul
mkdir microservices\api-gateway\src\main\java\com\example\attendancesystem\gateway\route 2>nul

echo Microservices structure created successfully!
echo.
echo Services created:
for %%s in (%SERVICES%) do (
    echo - %%s
)
echo.
echo Next steps:
echo 1. Create pom.xml files for each service
echo 2. Create application classes
echo 3. Create models and repositories
echo 4. Implement gRPC services
echo 5. Create Docker configurations
