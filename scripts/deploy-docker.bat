@echo off
REM Attendance Management System - Docker Deployment Script for Windows
REM This script handles building and deploying all services in Docker containers

setlocal enabledelayedexpansion

REM Configuration
set ENVIRONMENT=%1
if "%ENVIRONMENT%"=="" set ENVIRONMENT=dev
set ACTION=%2
if "%ACTION%"=="" set ACTION=deploy
set COMPOSE_FILE=""
set ENV_FILE=""

REM Colors (using echo with special characters)
set "RED=[91m"
set "GREEN=[92m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

echo %BLUE%[INFO]%NC% Starting Attendance Management System Docker Deployment
echo %BLUE%[INFO]%NC% Environment: %ENVIRONMENT%
echo.

REM Function to check prerequisites
echo %BLUE%[INFO]%NC% Checking prerequisites...

REM Check if Docker is installed and running
docker --version >nul 2>&1
if errorlevel 1 (
    echo %RED%[ERROR]%NC% Docker is not installed. Please install Docker Desktop first.
    exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
    echo %RED%[ERROR]%NC% Docker is not running. Please start Docker Desktop first.
    exit /b 1
)

REM Check if Docker Compose is available
docker-compose --version >nul 2>&1
if errorlevel 1 (
    docker compose version >nul 2>&1
    if errorlevel 1 (
        echo %RED%[ERROR]%NC% Docker Compose is not available. Please install Docker Compose.
        exit /b 1
    )
    set DOCKER_COMPOSE_CMD=docker compose
) else (
    set DOCKER_COMPOSE_CMD=docker-compose
)

echo %GREEN%[SUCCESS]%NC% Prerequisites check passed

REM Set environment
echo %BLUE%[INFO]%NC% Setting up environment: %ENVIRONMENT%

if "%ENVIRONMENT%"=="dev" (
    set COMPOSE_FILE=docker-compose.yml
    set ENV_FILE=.env.dev
) else if "%ENVIRONMENT%"=="development" (
    set COMPOSE_FILE=docker-compose.yml
    set ENV_FILE=.env.dev
) else if "%ENVIRONMENT%"=="prod" (
    set COMPOSE_FILE=docker-compose.prod.yml
    set ENV_FILE=.env.prod
) else if "%ENVIRONMENT%"=="production" (
    set COMPOSE_FILE=docker-compose.prod.yml
    set ENV_FILE=.env.prod
) else (
    echo %RED%[ERROR]%NC% Invalid environment: %ENVIRONMENT%. Use 'dev' or 'prod'
    exit /b 1
)

REM Copy environment file if .env doesn't exist
if not exist ".env" (
    if exist "%ENV_FILE%" (
        echo %BLUE%[INFO]%NC% Copying %ENV_FILE% to .env
        copy "%ENV_FILE%" ".env" >nul
    ) else (
        echo %YELLOW%[WARNING]%NC% Environment file %ENV_FILE% not found. Using defaults.
    )
)

echo %GREEN%[SUCCESS]%NC% Environment set to %ENVIRONMENT%

REM Execute action
if "%ACTION%"=="build" goto BUILD
if "%ACTION%"=="start" goto START
if "%ACTION%"=="stop" goto STOP
if "%ACTION%"=="restart" goto RESTART
if "%ACTION%"=="status" goto STATUS
if "%ACTION%"=="cleanup" goto CLEANUP
if "%ACTION%"=="deploy" goto DEPLOY
goto USAGE

:BUILD
echo %BLUE%[INFO]%NC% Building Docker images...

echo %BLUE%[INFO]%NC% Building backend image...
docker build -t ams-backend:latest .
if errorlevel 1 (
    echo %RED%[ERROR]%NC% Failed to build backend image
    exit /b 1
)

echo %BLUE%[INFO]%NC% Building admin panel image...
docker build -t ams-admin-panel:latest ./admin-panel
if errorlevel 1 (
    echo %RED%[ERROR]%NC% Failed to build admin panel image
    exit /b 1
)

echo %BLUE%[INFO]%NC% Building entity dashboard image...
docker build -t ams-entity-dashboard:latest ./entity-dashboard
if errorlevel 1 (
    echo %RED%[ERROR]%NC% Failed to build entity dashboard image
    exit /b 1
)

echo %BLUE%[INFO]%NC% Building public menu image...
docker build -t ams-public-menu:latest ./public-menu
if errorlevel 1 (
    echo %RED%[ERROR]%NC% Failed to build public menu image
    exit /b 1
)

echo %GREEN%[SUCCESS]%NC% All images built successfully
goto END

:START
echo %BLUE%[INFO]%NC% Starting services with %COMPOSE_FILE%...
%DOCKER_COMPOSE_CMD% -f "%COMPOSE_FILE%" up -d
if errorlevel 1 (
    echo %RED%[ERROR]%NC% Failed to start services
    exit /b 1
)
echo %GREEN%[SUCCESS]%NC% Services started successfully
goto HEALTH_CHECK

:STOP
echo %BLUE%[INFO]%NC% Stopping services...
%DOCKER_COMPOSE_CMD% -f "%COMPOSE_FILE%" down
echo %GREEN%[SUCCESS]%NC% Services stopped
goto END

:RESTART
echo %BLUE%[INFO]%NC% Restarting services...
%DOCKER_COMPOSE_CMD% -f "%COMPOSE_FILE%" down
%DOCKER_COMPOSE_CMD% -f "%COMPOSE_FILE%" up -d
if errorlevel 1 (
    echo %RED%[ERROR]%NC% Failed to restart services
    exit /b 1
)
echo %GREEN%[SUCCESS]%NC% Services restarted successfully
goto HEALTH_CHECK

:STATUS
echo %BLUE%[INFO]%NC% Service Status:
echo.
%DOCKER_COMPOSE_CMD% -f "%COMPOSE_FILE%" ps
echo.
echo %BLUE%[INFO]%NC% Service URLs:
echo   Backend API: http://localhost:8080
echo   Admin Panel: http://localhost:3001
echo   Entity Dashboard: http://localhost:3002
echo   Public Menu: http://localhost:3003
echo   Grafana: http://localhost:3000 (admin/admin123)
echo   Prometheus: http://localhost:9090
echo   Zipkin: http://localhost:9411
goto END

:CLEANUP
echo %BLUE%[INFO]%NC% Cleaning up Docker resources...
docker image prune -f
if "%ENVIRONMENT%"=="dev" (
    docker volume prune -f
)
echo %GREEN%[SUCCESS]%NC% Cleanup completed
goto END

:DEPLOY
call :BUILD
if errorlevel 1 exit /b 1
call :START
if errorlevel 1 exit /b 1
goto HEALTH_CHECK

:HEALTH_CHECK
echo %BLUE%[INFO]%NC% Checking service health...
timeout /t 10 /nobreak >nul
echo %GREEN%[SUCCESS]%NC% Health check completed
goto STATUS

:USAGE
echo Usage: %0 [dev^|prod] [build^|start^|stop^|restart^|status^|cleanup^|deploy]
echo.
echo Commands:
echo   build    - Build all Docker images
echo   start    - Start all services
echo   stop     - Stop all services
echo   restart  - Restart all services
echo   status   - Show service status
echo   cleanup  - Clean up Docker resources
echo   deploy   - Build and start all services (default)
exit /b 1

:END
echo %GREEN%[SUCCESS]%NC% Operation completed successfully
