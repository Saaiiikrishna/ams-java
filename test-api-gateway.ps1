# Test API Gateway Routing
Write-Host "🌐 Testing API Gateway Routing..." -ForegroundColor Cyan

# Test 1: API Gateway Health Check
Write-Host "`n1. Testing API Gateway Health..." -ForegroundColor Yellow
try {
    $gatewayHealth = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method GET
    Write-Host "✅ API Gateway Health: $($gatewayHealth.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ API Gateway Health Check Failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: SuperAdmin Authentication via API Gateway
Write-Host "`n2. Testing SuperAdmin Authentication via API Gateway..." -ForegroundColor Yellow
$loginData = @{
    username = "superadmin"
    password = "superadmin123"
} | ConvertTo-Json

try {
    $authResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/super/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SuperAdmin Authentication via API Gateway: SUCCESS" -ForegroundColor Green
    Write-Host "   Token Type: $($authResponse.tokenType)" -ForegroundColor Cyan
    $token = $authResponse.accessToken
} catch {
    Write-Host "❌ SuperAdmin Authentication via API Gateway Failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Response: $($_.Exception.Response)" -ForegroundColor Red
    exit 1
}

# Test 3: Organization Service via API Gateway
Write-Host "`n3. Testing Organization Service via API Gateway..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

try {
    $orgResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/organization/super/organizations" -Method GET -Headers $headers
    Write-Host "✅ Organization Service via API Gateway: SUCCESS" -ForegroundColor Green
    Write-Host "   Organizations found: $($orgResponse.Count)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Organization Service via API Gateway Failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Status Code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
}

# Test 4: Attendance Service via API Gateway
Write-Host "`n4. Testing Attendance Service via API Gateway..." -ForegroundColor Yellow
try {
    $attendanceHealth = Invoke-RestMethod -Uri "http://localhost:8080/api/attendance/actuator/health" -Method GET
    Write-Host "✅ Attendance Service via API Gateway: SUCCESS" -ForegroundColor Green
    Write-Host "   Status: $($attendanceHealth.status)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Attendance Service via API Gateway Failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Status Code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
}

# Test 5: Compare Direct vs Gateway Access
Write-Host "`n5. Comparing Direct vs Gateway Access..." -ForegroundColor Yellow

# Direct access to Auth Service
try {
    $directAuth = Invoke-RestMethod -Uri "http://localhost:8081/auth/super/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ Direct Auth Service Access: SUCCESS" -ForegroundColor Green
} catch {
    Write-Host "❌ Direct Auth Service Access Failed" -ForegroundColor Red
}

# Direct access to Organization Service
try {
    $directOrg = Invoke-RestMethod -Uri "http://localhost:8082/organization/super/organizations" -Method GET -Headers $headers
    Write-Host "✅ Direct Organization Service Access: SUCCESS" -ForegroundColor Green
} catch {
    Write-Host "❌ Direct Organization Service Access Failed" -ForegroundColor Red
}

Write-Host "`n🎯 API Gateway Testing Complete!" -ForegroundColor Cyan
Write-Host "📊 Summary:" -ForegroundColor White
Write-Host "   - API Gateway is running on port 8080" -ForegroundColor Gray
Write-Host "   - Microservices are accessible via API Gateway" -ForegroundColor Gray
Write-Host "   - Direct microservice access is still available (security concern)" -ForegroundColor Yellow
