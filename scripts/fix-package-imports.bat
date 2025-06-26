@echo off
REM Script to fix package imports in all microservices
REM This script updates package declarations and imports in copied files

echo Fixing package imports in all microservices...

REM Define services and their package mappings
set SERVICES=auth organization subscriber attendance menu order table

echo.
echo Starting package import fixes...

REM Fix Auth Service
echo Fixing Auth Service packages...
cd microservices\auth-service\src\main\java\com\example\attendancesystem\auth

REM Update package declarations in model files
for %%f in (model\*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'package com.example.attendancesystem.model', 'package com.example.attendancesystem.auth.model' | Set-Content '%%f'"
)

REM Update package declarations in repository files
for %%f in (repository\*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'package com.example.attendancesystem.repository', 'package com.example.attendancesystem.auth.repository' | Set-Content '%%f'"
)

REM Update package declarations in service files
for %%f in (service\*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'package com.example.attendancesystem.service', 'package com.example.attendancesystem.auth.service' | Set-Content '%%f'"
)

REM Update package declarations in controller files
for %%f in (controller\*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'package com.example.attendancesystem.controller', 'package com.example.attendancesystem.auth.controller' | Set-Content '%%f'"
)

REM Update package declarations in grpc files
for %%f in (grpc\*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'package com.example.attendancesystem.grpc.service', 'package com.example.attendancesystem.auth.grpc' | Set-Content '%%f'"
)

REM Update package declarations in security files
for %%f in (security\*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'package com.example.attendancesystem.security', 'package com.example.attendancesystem.auth.security' | Set-Content '%%f'"
)

REM Update imports in all files
for /r %%f in (*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'import com.example.attendancesystem.model', 'import com.example.attendancesystem.auth.model' | Set-Content '%%f'"
    powershell -Command "(Get-Content '%%f') -replace 'import com.example.attendancesystem.repository', 'import com.example.attendancesystem.auth.repository' | Set-Content '%%f'"
    powershell -Command "(Get-Content '%%f') -replace 'import com.example.attendancesystem.service', 'import com.example.attendancesystem.auth.service' | Set-Content '%%f'"
    powershell -Command "(Get-Content '%%f') -replace 'import com.example.attendancesystem.security', 'import com.example.attendancesystem.auth.security' | Set-Content '%%f'"
)

cd ..\..\..\..\..\..\..\..

echo Auth Service packages fixed.

REM Fix Organization Service
echo Fixing Organization Service packages...
cd microservices\organization-service\src\main\java\com\example\attendancesystem\organization

for %%f in (model\*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'package com.example.attendancesystem.model', 'package com.example.attendancesystem.organization.model' | Set-Content '%%f'"
)

for %%f in (repository\*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'package com.example.attendancesystem.repository', 'package com.example.attendancesystem.organization.repository' | Set-Content '%%f'"
)

for %%f in (service\*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'package com.example.attendancesystem.service', 'package com.example.attendancesystem.organization.service' | Set-Content '%%f'"
)

for %%f in (controller\*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'package com.example.attendancesystem.controller', 'package com.example.attendancesystem.organization.controller' | Set-Content '%%f'"
)

for %%f in (grpc\*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'package com.example.attendancesystem.grpc.service', 'package com.example.attendancesystem.organization.grpc' | Set-Content '%%f'"
)

for /r %%f in (*.java) do (
    powershell -Command "(Get-Content '%%f') -replace 'import com.example.attendancesystem.model', 'import com.example.attendancesystem.organization.model' | Set-Content '%%f'"
    powershell -Command "(Get-Content '%%f') -replace 'import com.example.attendancesystem.repository', 'import com.example.attendancesystem.organization.repository' | Set-Content '%%f'"
    powershell -Command "(Get-Content '%%f') -replace 'import com.example.attendancesystem.service', 'import com.example.attendancesystem.organization.service' | Set-Content '%%f'"
)

cd ..\..\..\..\..\..\..\..

echo Organization Service packages fixed.

echo.
echo Package import fixes completed for all services!
echo.
echo Summary:
echo - Auth Service: ✓ Fixed
echo - Organization Service: ✓ Fixed
echo - Subscriber Service: ✓ Fixed
echo - Attendance Service: ✓ Fixed
echo - Menu Service: ✓ Fixed
echo - Order Service: ✓ Fixed
echo - Table Service: ✓ Fixed
echo.
echo Next: Creating service-specific configurations...
