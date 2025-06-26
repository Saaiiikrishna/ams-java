@echo off
REM Simple script to fix package imports in microservices

echo Fixing package imports in microservices...

REM Fix Auth Service
echo Fixing Auth Service...
cd microservices\auth-service\src\main\java\com\example\attendancesystem\auth

REM Fix package declarations
powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'package com.example.attendancesystem.model', 'package com.example.attendancesystem.auth.model' -replace 'package com.example.attendancesystem.repository', 'package com.example.attendancesystem.auth.repository' -replace 'package com.example.attendancesystem.service', 'package com.example.attendancesystem.auth.service' -replace 'package com.example.attendancesystem.controller', 'package com.example.attendancesystem.auth.controller' -replace 'package com.example.attendancesystem.grpc.service', 'package com.example.attendancesystem.auth.grpc' -replace 'package com.example.attendancesystem.security', 'package com.example.attendancesystem.auth.security' | Set-Content $_.FullName }"

REM Fix imports
powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'import com.example.attendancesystem.model', 'import com.example.attendancesystem.auth.model' -replace 'import com.example.attendancesystem.repository', 'import com.example.attendancesystem.auth.repository' -replace 'import com.example.attendancesystem.service', 'import com.example.attendancesystem.auth.service' -replace 'import com.example.attendancesystem.security', 'import com.example.attendancesystem.auth.security' | Set-Content $_.FullName }"

cd ..\..\..\..\..\..\..\..

echo Auth Service fixed.

REM Fix Organization Service
echo Fixing Organization Service...
cd microservices\organization-service\src\main\java\com\example\attendancesystem\organization

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'package com.example.attendancesystem.model', 'package com.example.attendancesystem.organization.model' -replace 'package com.example.attendancesystem.repository', 'package com.example.attendancesystem.organization.repository' -replace 'package com.example.attendancesystem.service', 'package com.example.attendancesystem.organization.service' -replace 'package com.example.attendancesystem.controller', 'package com.example.attendancesystem.organization.controller' -replace 'package com.example.attendancesystem.grpc.service', 'package com.example.attendancesystem.organization.grpc' | Set-Content $_.FullName }"

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'import com.example.attendancesystem.model', 'import com.example.attendancesystem.organization.model' -replace 'import com.example.attendancesystem.repository', 'import com.example.attendancesystem.organization.repository' -replace 'import com.example.attendancesystem.service', 'import com.example.attendancesystem.organization.service' | Set-Content $_.FullName }"

cd ..\..\..\..\..\..\..\..

echo Organization Service fixed.

REM Fix Subscriber Service
echo Fixing Subscriber Service...
cd microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'package com.example.attendancesystem.model', 'package com.example.attendancesystem.subscriber.model' -replace 'package com.example.attendancesystem.repository', 'package com.example.attendancesystem.subscriber.repository' -replace 'package com.example.attendancesystem.service', 'package com.example.attendancesystem.subscriber.service' -replace 'package com.example.attendancesystem.controller', 'package com.example.attendancesystem.subscriber.controller' -replace 'package com.example.attendancesystem.grpc.service', 'package com.example.attendancesystem.subscriber.grpc' | Set-Content $_.FullName }"

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'import com.example.attendancesystem.model', 'import com.example.attendancesystem.subscriber.model' -replace 'import com.example.attendancesystem.repository', 'import com.example.attendancesystem.subscriber.repository' -replace 'import com.example.attendancesystem.service', 'import com.example.attendancesystem.subscriber.service' | Set-Content $_.FullName }"

cd ..\..\..\..\..\..\..\..

echo Subscriber Service fixed.

REM Fix Attendance Service
echo Fixing Attendance Service...
cd microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'package com.example.attendancesystem.model', 'package com.example.attendancesystem.attendance.model' -replace 'package com.example.attendancesystem.repository', 'package com.example.attendancesystem.attendance.repository' -replace 'package com.example.attendancesystem.service', 'package com.example.attendancesystem.attendance.service' -replace 'package com.example.attendancesystem.controller', 'package com.example.attendancesystem.attendance.controller' -replace 'package com.example.attendancesystem.grpc.service', 'package com.example.attendancesystem.attendance.grpc' -replace 'package com.example.attendancesystem.facerecognition', 'package com.example.attendancesystem.attendance.facerecognition' | Set-Content $_.FullName }"

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'import com.example.attendancesystem.model', 'import com.example.attendancesystem.attendance.model' -replace 'import com.example.attendancesystem.repository', 'import com.example.attendancesystem.attendance.repository' -replace 'import com.example.attendancesystem.service', 'import com.example.attendancesystem.attendance.service' -replace 'import com.example.attendancesystem.facerecognition', 'import com.example.attendancesystem.attendance.facerecognition' | Set-Content $_.FullName }"

cd ..\..\..\..\..\..\..\..

echo Attendance Service fixed.

REM Fix Menu Service
echo Fixing Menu Service...
cd microservices\menu-service\src\main\java\com\example\attendancesystem\menu

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'package com.example.attendancesystem.model', 'package com.example.attendancesystem.menu.model' -replace 'package com.example.attendancesystem.repository', 'package com.example.attendancesystem.menu.repository' -replace 'package com.example.attendancesystem.service', 'package com.example.attendancesystem.menu.service' -replace 'package com.example.attendancesystem.controller', 'package com.example.attendancesystem.menu.controller' -replace 'package com.example.attendancesystem.grpc.service', 'package com.example.attendancesystem.menu.grpc' | Set-Content $_.FullName }"

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'import com.example.attendancesystem.model', 'import com.example.attendancesystem.menu.model' -replace 'import com.example.attendancesystem.repository', 'import com.example.attendancesystem.menu.repository' -replace 'import com.example.attendancesystem.service', 'import com.example.attendancesystem.menu.service' | Set-Content $_.FullName }"

cd ..\..\..\..\..\..\..\..

echo Menu Service fixed.

REM Fix Order Service
echo Fixing Order Service...
cd microservices\order-service\src\main\java\com\example\attendancesystem\order

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'package com.example.attendancesystem.model', 'package com.example.attendancesystem.order.model' -replace 'package com.example.attendancesystem.repository', 'package com.example.attendancesystem.order.repository' -replace 'package com.example.attendancesystem.service', 'package com.example.attendancesystem.order.service' -replace 'package com.example.attendancesystem.controller', 'package com.example.attendancesystem.order.controller' -replace 'package com.example.attendancesystem.grpc.service', 'package com.example.attendancesystem.order.grpc' | Set-Content $_.FullName }"

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'import com.example.attendancesystem.model', 'import com.example.attendancesystem.order.model' -replace 'import com.example.attendancesystem.repository', 'import com.example.attendancesystem.order.repository' -replace 'import com.example.attendancesystem.service', 'import com.example.attendancesystem.order.service' | Set-Content $_.FullName }"

cd ..\..\..\..\..\..\..\..

echo Order Service fixed.

REM Fix Table Service
echo Fixing Table Service...
cd microservices\table-service\src\main\java\com\example\attendancesystem\table

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'package com.example.attendancesystem.model', 'package com.example.attendancesystem.table.model' -replace 'package com.example.attendancesystem.repository', 'package com.example.attendancesystem.table.repository' -replace 'package com.example.attendancesystem.service', 'package com.example.attendancesystem.table.service' -replace 'package com.example.attendancesystem.controller', 'package com.example.attendancesystem.table.controller' -replace 'package com.example.attendancesystem.grpc.service', 'package com.example.attendancesystem.table.grpc' | Set-Content $_.FullName }"

powershell -Command "Get-ChildItem -Recurse -Filter '*.java' | ForEach-Object { (Get-Content $_.FullName) -replace 'import com.example.attendancesystem.model', 'import com.example.attendancesystem.table.model' -replace 'import com.example.attendancesystem.repository', 'import com.example.attendancesystem.table.repository' -replace 'import com.example.attendancesystem.service', 'import com.example.attendancesystem.table.service' | Set-Content $_.FullName }"

cd ..\..\..\..\..\..\..\..

echo Table Service fixed.

echo.
echo All package imports fixed successfully!
echo.
echo Summary:
echo - Auth Service: ✓ Fixed
echo - Organization Service: ✓ Fixed  
echo - Subscriber Service: ✓ Fixed
echo - Attendance Service: ✓ Fixed
echo - Menu Service: ✓ Fixed
echo - Order Service: ✓ Fixed
echo - Table Service: ✓ Fixed
