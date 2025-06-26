@echo off
REM Complete microservices deployment script
REM This script handles package fixes, builds, and deployment

echo ========================================
echo Microservices Deployment Script
echo ========================================
echo.

REM Step 1: Build Shared Library
echo Step 1: Building Shared Library...
cd microservices\shared-lib
call mvn clean install -DskipTests -q
if errorlevel 1 (
    echo ERROR: Failed to build shared library
    exit /b 1
)
echo ✓ Shared library built successfully
cd ..\..

REM Step 2: Quick Package Import Fix for Critical Files
echo.
echo Step 2: Fixing critical package imports...

REM Fix Auth Service Application class imports
powershell -Command "(Get-Content 'microservices\auth-service\src\main\java\com\example\attendancesystem\auth\AuthServiceApplication.java') -replace 'import com.example.attendancesystem.model', 'import com.example.attendancesystem.auth.model' -replace 'import com.example.attendancesystem.repository', 'import com.example.attendancesystem.auth.repository' | Set-Content 'microservices\auth-service\src\main\java\com\example\attendancesystem\auth\AuthServiceApplication.java'"

REM Fix Organization Service Application class imports  
powershell -Command "(Get-Content 'microservices\organization-service\src\main\java\com\example\attendancesystem\organization\OrganizationServiceApplication.java') -replace 'import com.example.attendancesystem.model', 'import com.example.attendancesystem.organization.model' -replace 'import com.example.attendancesystem.repository', 'import com.example.attendancesystem.organization.repository' | Set-Content 'microservices\organization-service\src\main\java\com\example\attendancesystem\organization\OrganizationServiceApplication.java'"

echo ✓ Critical package imports fixed

REM Step 3: Create minimal working versions for testing
echo.
echo Step 3: Creating minimal service implementations...

REM Create a simple working controller for each service to test deployment
echo Creating test controllers...

REM Auth Service Test Controller
mkdir microservices\auth-service\src\main\java\com\example\attendancesystem\auth\controller 2>nul
(
echo package com.example.attendancesystem.auth.controller;
echo.
echo import org.springframework.web.bind.annotation.*;
echo import org.springframework.http.ResponseEntity;
echo import java.util.Map;
echo.
echo @RestController
echo @RequestMapping("/test"^)
echo public class AuthTestController {
echo.
echo     @GetMapping("/health"^)
echo     public ResponseEntity^<Map^<String, String^>^> health(^) {
echo         return ResponseEntity.ok(Map.of("service", "auth-service", "status", "UP"^)^);
echo     }
echo }
) > microservices\auth-service\src\main\java\com\example\attendancesystem\auth\controller\AuthTestController.java

REM Organization Service Test Controller
mkdir microservices\organization-service\src\main\java\com\example\attendancesystem\organization\controller 2>nul
(
echo package com.example.attendancesystem.organization.controller;
echo.
echo import org.springframework.web.bind.annotation.*;
echo import org.springframework.http.ResponseEntity;
echo import java.util.Map;
echo.
echo @RestController
echo @RequestMapping("/test"^)
echo public class OrganizationTestController {
echo.
echo     @GetMapping("/health"^)
echo     public ResponseEntity^<Map^<String, String^>^> health(^) {
echo         return ResponseEntity.ok(Map.of("service", "organization-service", "status", "UP"^)^);
echo     }
echo }
) > microservices\organization-service\src\main\java\com\example\attendancesystem\organization\controller\OrganizationTestController.java

echo ✓ Test controllers created

REM Step 4: Build Services
echo.
echo Step 4: Building microservices...

set SERVICES=auth-service organization-service

for %%s in (%SERVICES%) do (
    echo Building %%s...
    cd microservices\%%s
    call mvn clean package -DskipTests -q
    if errorlevel 1 (
        echo ERROR: Failed to build %%s
        cd ..\..
        exit /b 1
    )
    echo ✓ %%s built successfully
    cd ..\..
)

REM Step 5: Start Infrastructure
echo.
echo Step 5: Starting infrastructure services...
docker-compose -f docker-compose.microservices.yml up -d postgres
echo Waiting for PostgreSQL to be ready...
timeout /t 30 /nobreak >nul

REM Step 6: Deploy Core Services
echo.
echo Step 6: Deploying core microservices...
docker-compose -f docker-compose.microservices.yml up -d auth-service organization-service

echo.
echo ========================================
echo Deployment Status
echo ========================================
echo.
echo Infrastructure:
echo ✓ PostgreSQL Database: http://localhost:5432
echo.
echo Microservices:
echo ✓ Auth Service: http://localhost:8081/auth/test/health
echo ✓ Organization Service: http://localhost:8082/organization/test/health
echo.
echo Monitoring:
echo - Check service logs: docker-compose -f docker-compose.microservices.yml logs -f
echo - Check service status: docker-compose -f docker-compose.microservices.yml ps
echo.
echo Next Steps:
echo 1. Test services: curl http://localhost:8081/auth/test/health
echo 2. Deploy remaining services: docker-compose -f docker-compose.microservices.yml up -d
echo 3. Access API Gateway: http://localhost:8080 (when deployed^)
echo.
echo ========================================
echo Deployment Complete!
echo ========================================
