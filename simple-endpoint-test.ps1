# Simple Endpoint Testing Script
Write-Host "🎯 MICROSERVICES ENDPOINT TESTING" -ForegroundColor Cyan

# Test 1: SuperAdmin Login via API Gateway
Write-Host "`n1. Testing SuperAdmin Login via API Gateway..." -ForegroundColor Yellow
try {
    $loginData = '{"username":"superadmin","password":"admin123"}'
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/super/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SuperAdmin Login: SUCCESS" -ForegroundColor Green
    $superAdminToken = $response.jwt
    Write-Host "   Token: $($superAdminToken.Substring(0,50))..." -ForegroundColor Cyan
} catch {
    Write-Host "❌ SuperAdmin Login: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: SuperAdmin Login Direct
Write-Host "`n2. Testing SuperAdmin Login Direct..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/auth/super/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SuperAdmin Login Direct: SUCCESS" -ForegroundColor Green
} catch {
    Write-Host "❌ SuperAdmin Login Direct: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Create Entity via API Gateway
if ($superAdminToken) {
    Write-Host "`n3. Testing Create Entity via API Gateway..." -ForegroundColor Yellow
    try {
        $entityData = '{"name":"Anu house","description":"Test entity","address":"123 Test St","contactEmail":"anu@example.com","contactPhone":"1234567890"}'
        $headers = @{"Authorization" = "Bearer $superAdminToken"}
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/organization/super/organizations" -Method POST -Body $entityData -ContentType "application/json" -Headers $headers
        Write-Host "✅ Create Entity: SUCCESS" -ForegroundColor Green
        $entityId = $response.id
        Write-Host "   Entity ID: $entityId" -ForegroundColor Cyan
    } catch {
        Write-Host "❌ Create Entity: FAILED - $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Test 4: Create Entity Direct
if ($superAdminToken) {
    Write-Host "`n4. Testing Create Entity Direct..." -ForegroundColor Yellow
    try {
        $headers = @{"Authorization" = "Bearer $superAdminToken"}
        $response = Invoke-RestMethod -Uri "http://localhost:8082/organization/super/organizations" -Method POST -Body $entityData -ContentType "application/json" -Headers $headers
        Write-Host "✅ Create Entity Direct: SUCCESS" -ForegroundColor Green
    } catch {
        Write-Host "❌ Create Entity Direct: FAILED - $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Test 5: Create EntityAdmin via API Gateway
if ($superAdminToken -and $entityId) {
    Write-Host "`n5. Testing Create EntityAdmin via API Gateway..." -ForegroundColor Yellow
    try {
        $entityAdminData = "{`"username`":`"Anu`",`"password`":`"admin123`",`"firstName`":`"Anu`",`"lastName`":`"Admin`",`"email`":`"anu.admin@example.com`",`"organizationId`":$entityId}"
        $headers = @{"Authorization" = "Bearer $superAdminToken"}
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/organization/super/entity-admins" -Method POST -Body $entityAdminData -ContentType "application/json" -Headers $headers
        Write-Host "✅ Create EntityAdmin: SUCCESS" -ForegroundColor Green
        Write-Host "   EntityAdmin: $($response.username)" -ForegroundColor Cyan
    } catch {
        Write-Host "❌ Create EntityAdmin: FAILED - $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Test 6: EntityAdmin Login via API Gateway (Modern Auth v2)
Write-Host "`n6. Testing EntityAdmin Login via API Gateway..." -ForegroundColor Yellow
try {
    $entityAdminLoginData = '{"username":"FinalTestAdmin","password":"admin123"}'
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/v2/auth/login" -Method POST -Body $entityAdminLoginData -ContentType "application/json"
    Write-Host "✅ EntityAdmin Login: SUCCESS" -ForegroundColor Green
    $entityAdminToken = $response.accessToken
    Write-Host "   Token: $($entityAdminToken.Substring(0,50))..." -ForegroundColor Cyan
} catch {
    Write-Host "❌ EntityAdmin Login: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Test 7: EntityAdmin Login Direct (Modern Auth v2)
Write-Host "`n7. Testing EntityAdmin Login Direct..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/auth/api/v2/auth/login" -Method POST -Body $entityAdminLoginData -ContentType "application/json"
    Write-Host "✅ EntityAdmin Login Direct: SUCCESS" -ForegroundColor Green
} catch {
    Write-Host "❌ EntityAdmin Login Direct: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Test 8: Sessions Endpoint via API Gateway
Write-Host "`n8. Testing Sessions Endpoint via API Gateway..." -ForegroundColor Yellow
try {
    $headers = @{"Authorization" = "Bearer $superAdminToken"}
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/attendance/subscriber/checkin/sessions" -Headers $headers
    Write-Host "✅ Sessions Endpoint: SUCCESS" -ForegroundColor Green
    Write-Host "   Sessions: $($response.count)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Sessions Endpoint: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Test 9: Sessions Endpoint Direct
Write-Host "`n9. Testing Sessions Endpoint Direct..." -ForegroundColor Yellow
try {
    $headers = @{"Authorization" = "Bearer $superAdminToken"}
    $response = Invoke-RestMethod -Uri "http://localhost:8084/attendance/subscriber/checkin/sessions" -Headers $headers
    Write-Host "✅ Sessions Endpoint Direct: SUCCESS" -ForegroundColor Green
    Write-Host "   Sessions: $($response.count)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Sessions Endpoint Direct: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Test 10: Check All Services Health
Write-Host "`n10. Testing All Services Health..." -ForegroundColor Yellow

$services = @(
    @{Name="Auth Service"; Url="http://localhost:8081/auth/actuator/health"},
    @{Name="Organization Service"; Url="http://localhost:8082/organization/actuator/health"},
    @{Name="User Service"; Url="http://localhost:8083/user/actuator/health"},
    @{Name="Attendance Service"; Url="http://localhost:8084/attendance/actuator/health"},
    @{Name="API Gateway"; Url="http://localhost:8080/actuator/health"}
)

foreach ($service in $services) {
    try {
        $response = Invoke-RestMethod -Uri $service.Url
        Write-Host "✅ $($service.Name): $($response.status)" -ForegroundColor Green
    } catch {
        Write-Host "❌ $($service.Name): FAILED" -ForegroundColor Red
    }
}

Write-Host "`n🎯 TESTING COMPLETE!" -ForegroundColor Cyan
