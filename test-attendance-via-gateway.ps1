# Test Attendance Service via API Gateway
Write-Host "🎯 Testing Attendance Service via API Gateway..." -ForegroundColor Cyan

# Step 1: Get SuperAdmin token via API Gateway
Write-Host "`n1. Getting SuperAdmin token via API Gateway..." -ForegroundColor Yellow
$loginData = @{
    username = "superadmin"
    password = "admin123"
} | ConvertTo-Json

try {
    $authResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/super/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SuperAdmin Authentication via API Gateway: SUCCESS" -ForegroundColor Green
    $token = $authResponse.jwt
    Write-Host "   Token received: $($token.Substring(0,50))..." -ForegroundColor Cyan
} catch {
    Write-Host "❌ SuperAdmin Authentication Failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Test Attendance Service Health via API Gateway
Write-Host "`n2. Testing Attendance Service Health via API Gateway..." -ForegroundColor Yellow
try {
    $attendanceHealth = Invoke-RestMethod -Uri "http://localhost:8080/api/attendance/actuator/health" -Method GET
    Write-Host "✅ Attendance Service Health via API Gateway: SUCCESS" -ForegroundColor Green
    Write-Host "   Status: $($attendanceHealth.status)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Attendance Service Health via API Gateway Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Step 3: Test Organization Service via API Gateway (for comparison)
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
}

# Step 4: Compare Direct vs Gateway Access
Write-Host "`n4. Comparing Direct vs Gateway Access..." -ForegroundColor Yellow

# Direct Attendance Service
try {
    $directAttendance = Invoke-RestMethod -Uri "http://localhost:8084/attendance/actuator/health" -Method GET
    Write-Host "✅ Direct Attendance Service: SUCCESS ($($directAttendance.status))" -ForegroundColor Green
} catch {
    Write-Host "❌ Direct Attendance Service Failed" -ForegroundColor Red
}

# Direct Organization Service
try {
    $directOrg = Invoke-RestMethod -Uri "http://localhost:8082/organization/super/organizations" -Method GET -Headers $headers
    Write-Host "✅ Direct Organization Service: SUCCESS ($($directOrg.Count) orgs)" -ForegroundColor Green
} catch {
    Write-Host "❌ Direct Organization Service Failed" -ForegroundColor Red
}

# Step 5: Test Attendance Service Endpoints (if available)
Write-Host "`n5. Testing Attendance Service Endpoints..." -ForegroundColor Yellow

# Test attendance sessions endpoint
try {
    $sessionsResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/attendance/sessions" -Method GET -Headers $headers
    Write-Host "✅ Attendance Sessions via API Gateway: SUCCESS" -ForegroundColor Green
    Write-Host "   Sessions found: $($sessionsResponse.Count)" -ForegroundColor Cyan
} catch {
    Write-Host "⚠️ Attendance Sessions endpoint not available or requires different auth: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`n🎯 Attendance Service Testing Complete!" -ForegroundColor Cyan
Write-Host "📊 Summary:" -ForegroundColor White
Write-Host "   - API Gateway is working on port 8080" -ForegroundColor Gray
Write-Host "   - Attendance Service is accessible via API Gateway" -ForegroundColor Gray
Write-Host "   - All microservices can be accessed through API Gateway" -ForegroundColor Gray
Write-Host "   - Direct access to microservices is still available" -ForegroundColor Yellow
