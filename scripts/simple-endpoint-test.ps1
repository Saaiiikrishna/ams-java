# Simple Microservices Endpoint Testing Script

Write-Host "🚀 Starting Microservices Endpoint Testing" -ForegroundColor Blue

# Test 1: SuperAdmin Login
Write-Host "`n🔐 Testing SuperAdmin Login..." -ForegroundColor Yellow
try {
    $body = @{
        username = "superadmin"
        password = "admin123"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/super/login" -Method Post -Body $body -ContentType "application/json"
    Write-Host "✅ SuperAdmin Login: SUCCESS" -ForegroundColor Green
    $token = $response.jwt
    Write-Host "   Token received (length: $($token.Length))" -ForegroundColor Gray
} catch {
    Write-Host "❌ SuperAdmin Login: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
    $token = $null
}

# Test 2: SuperAdmin Monitoring Dashboard
Write-Host "`n📊 Testing SuperAdmin Monitoring Dashboard..." -ForegroundColor Yellow
if ($token) {
    try {
        $headers = @{ "Authorization" = "Bearer $token" }
        $response = Invoke-RestMethod -Uri "http://localhost:8081/auth/super/monitoring/dashboard" -Headers $headers
        Write-Host "✅ Monitoring Dashboard: SUCCESS" -ForegroundColor Green
        Write-Host "   Services status retrieved" -ForegroundColor Gray
    } catch {
        Write-Host "❌ Monitoring Dashboard: FAILED" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
    }
} else {
    Write-Host "⏭️ Monitoring Dashboard: SKIPPED (no token)" -ForegroundColor Yellow
}

# Test 3: Create Organization
Write-Host "`n🏢 Testing Create Organization..." -ForegroundColor Yellow
if ($token) {
    try {
        $headers = @{ "Authorization" = "Bearer $token" }
        $orgBody = @{
            name = "Test Organization $(Get-Date -Format 'HHmmss')"
            description = "Test Description"
            address = "Test Address"
            contactEmail = "test@example.com"
            contactPhone = "1234567890"
        } | ConvertTo-Json

        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/organization/super/organizations" -Method Post -Headers $headers -Body $orgBody -ContentType "application/json"
        Write-Host "✅ Create Organization: SUCCESS" -ForegroundColor Green
        Write-Host "   Organization ID: $($response.organization.id)" -ForegroundColor Gray
        Write-Host "   Entity ID: $($response.organization.entityId)" -ForegroundColor Gray
        $orgId = $response.organization.id
    } catch {
        Write-Host "❌ Create Organization: FAILED" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
        $orgId = $null
    }
} else {
    Write-Host "⏭️ Create Organization: SKIPPED (no token)" -ForegroundColor Yellow
}

# Test 4: Create Entity Admin
Write-Host "`n👤 Testing Create Entity Admin..." -ForegroundColor Yellow
if ($token -and $orgId) {
    try {
        $headers = @{ "Authorization" = "Bearer $token" }
        $adminBody = @{
            organizationId = $orgId
            username = "testadmin$(Get-Date -Format 'HHmmss')"
            password = "admin123"
            email = "testadmin@example.com"
            firstName = "Test"
            lastName = "Admin"
        } | ConvertTo-Json

        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/organization/super/entity-admins" -Method Post -Headers $headers -Body $adminBody -ContentType "application/json"
        Write-Host "✅ Create Entity Admin: SUCCESS" -ForegroundColor Green
        Write-Host "   Entity Admin created successfully" -ForegroundColor Gray
    } catch {
        Write-Host "❌ Create Entity Admin: FAILED" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
        Write-Host "   Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Gray
    }
} else {
    Write-Host "⏭️ Create Entity Admin: SKIPPED (no token or org)" -ForegroundColor Yellow
}

# Test 5: User Service - Get All Users
Write-Host "`n👥 Testing User Service - Get All Users..." -ForegroundColor Yellow
if ($token) {
    try {
        $headers = @{ 
            "Authorization" = "Bearer $token"
            "X-User-ID" = "1"
        }
        $response = Invoke-RestMethod -Uri "http://localhost:8083/user/api/users" -Headers $headers
        Write-Host "✅ Get All Users: SUCCESS" -ForegroundColor Green
        Write-Host "   Users retrieved successfully" -ForegroundColor Gray
    } catch {
        Write-Host "❌ Get All Users: FAILED" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
    }
} else {
    Write-Host "⏭️ Get All Users: SKIPPED (no token)" -ForegroundColor Yellow
}

# Test 6: User Service - Create Super Admin
Write-Host "`n👑 Testing User Service - Create Super Admin..." -ForegroundColor Yellow
if ($token) {
    try {
        $headers = @{ 
            "Authorization" = "Bearer $token"
            "X-User-ID" = "1"
        }
        $superAdminBody = @{
            username = "testsuperadmin$(Get-Date -Format 'HHmmss')"
            password = "admin123"
            email = "testsuperadmin@example.com"
            firstName = "Test"
            lastName = "SuperAdmin"
        } | ConvertTo-Json

        $response = Invoke-RestMethod -Uri "http://localhost:8083/user/api/users/super-admin" -Method Post -Headers $headers -Body $superAdminBody -ContentType "application/json"
        Write-Host "✅ Create Super Admin: SUCCESS" -ForegroundColor Green
        Write-Host "   Super Admin created successfully" -ForegroundColor Gray
    } catch {
        Write-Host "❌ Create Super Admin: FAILED" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
    }
} else {
    Write-Host "⏭️ Create Super Admin: SKIPPED (no token)" -ForegroundColor Yellow
}

# Test 7: Attendance Service Health
Write-Host "`n📊 Testing Attendance Service Health..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8084/attendance/actuator/health"
    Write-Host "✅ Attendance Service Health: SUCCESS" -ForegroundColor Green
    Write-Host "   Status: $($response.status)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Attendance Service Health: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
}

# Test 8: API Gateway Health
Write-Host "`n🌐 Testing API Gateway Health..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
    Write-Host "✅ API Gateway Health: SUCCESS" -ForegroundColor Green
    Write-Host "   Status: $($response.status)" -ForegroundColor Gray
} catch {
    Write-Host "❌ API Gateway Health: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
}

# Test 9: Organization Test Endpoints
Write-Host "`n🧪 Testing Organization Test Endpoints..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8082/organization/test/public"
    Write-Host "✅ Organization Test Public: SUCCESS" -ForegroundColor Green
    Write-Host "   Message: $($response.message)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Organization Test Public: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
}

# Test 10: User Service Health
Write-Host "`n👥 Testing User Service Health..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8083/user/actuator/health"
    Write-Host "✅ User Service Health: SUCCESS" -ForegroundColor Green
    Write-Host "   Status: $($response.status)" -ForegroundColor Gray
} catch {
    Write-Host "❌ User Service Health: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
}

Write-Host "`n📋 Testing Complete!" -ForegroundColor Blue
Write-Host "Review the results above to identify any failing endpoints." -ForegroundColor Blue
