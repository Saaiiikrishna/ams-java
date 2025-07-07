# Simple Microservices Endpoint Testing Script

Write-Host "Starting Microservices Endpoint Testing" -ForegroundColor Blue

# Test 1: SuperAdmin Login
Write-Host "`nTesting SuperAdmin Login..." -ForegroundColor Yellow
try {
    $body = @{
        username = "superadmin"
        password = "admin123"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/super/login" -Method Post -Body $body -ContentType "application/json"
    Write-Host "SUCCESS: SuperAdmin Login" -ForegroundColor Green
    $token = $response.jwt
    Write-Host "   Token received (length: $($token.Length))" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: SuperAdmin Login" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
    $token = $null
}

# Test 2: SuperAdmin Monitoring Dashboard
Write-Host "`nTesting SuperAdmin Monitoring Dashboard..." -ForegroundColor Yellow
if ($token) {
    try {
        $headers = @{ "Authorization" = "Bearer $token" }
        $response = Invoke-RestMethod -Uri "http://localhost:8081/auth/super/monitoring/dashboard" -Headers $headers
        Write-Host "SUCCESS: Monitoring Dashboard" -ForegroundColor Green
        Write-Host "   Services status retrieved" -ForegroundColor Gray
    } catch {
        Write-Host "FAILED: Monitoring Dashboard" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
    }
} else {
    Write-Host "SKIPPED: Monitoring Dashboard (no token)" -ForegroundColor Yellow
}

# Test 3: Create Organization
Write-Host "`nTesting Create Organization..." -ForegroundColor Yellow
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
        Write-Host "SUCCESS: Create Organization" -ForegroundColor Green
        Write-Host "   Organization ID: $($response.organization.id)" -ForegroundColor Gray
        Write-Host "   Entity ID: $($response.organization.entityId)" -ForegroundColor Gray
        $orgId = $response.organization.id
    } catch {
        Write-Host "FAILED: Create Organization" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
        $orgId = $null
    }
} else {
    Write-Host "SKIPPED: Create Organization (no token)" -ForegroundColor Yellow
}

# Test 4: Create Entity Admin
Write-Host "`nTesting Create Entity Admin..." -ForegroundColor Yellow
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
        Write-Host "SUCCESS: Create Entity Admin" -ForegroundColor Green
        Write-Host "   Entity Admin created successfully" -ForegroundColor Gray
    } catch {
        Write-Host "FAILED: Create Entity Admin" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
        Write-Host "   Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Gray
    }
} else {
    Write-Host "SKIPPED: Create Entity Admin (no token or org)" -ForegroundColor Yellow
}

# Test 5: User Service - Get All Users
Write-Host "`nTesting User Service - Get All Users..." -ForegroundColor Yellow
if ($token) {
    try {
        $headers = @{ 
            "Authorization" = "Bearer $token"
            "X-User-ID" = "1"
        }
        $response = Invoke-RestMethod -Uri "http://localhost:8083/user/api/users" -Headers $headers
        Write-Host "SUCCESS: Get All Users" -ForegroundColor Green
        Write-Host "   Users retrieved successfully" -ForegroundColor Gray
    } catch {
        Write-Host "FAILED: Get All Users" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
    }
} else {
    Write-Host "SKIPPED: Get All Users (no token)" -ForegroundColor Yellow
}

# Test 6: User Service - Create Super Admin
Write-Host "`nTesting User Service - Create Super Admin..." -ForegroundColor Yellow
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
        Write-Host "SUCCESS: Create Super Admin" -ForegroundColor Green
        Write-Host "   Super Admin created successfully" -ForegroundColor Gray
    } catch {
        Write-Host "FAILED: Create Super Admin" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
    }
} else {
    Write-Host "SKIPPED: Create Super Admin (no token)" -ForegroundColor Yellow
}

# Test 7: Attendance Service Health
Write-Host "`nTesting Attendance Service Health..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8084/attendance/actuator/health"
    Write-Host "SUCCESS: Attendance Service Health" -ForegroundColor Green
    Write-Host "   Status: $($response.status)" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: Attendance Service Health" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
}

# Test 8: API Gateway Health
Write-Host "`nTesting API Gateway Health..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
    Write-Host "SUCCESS: API Gateway Health" -ForegroundColor Green
    Write-Host "   Status: $($response.status)" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: API Gateway Health" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
}

# Test 9: Organization Test Endpoints
Write-Host "`nTesting Organization Test Endpoints..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8082/organization/test/public"
    Write-Host "SUCCESS: Organization Test Public" -ForegroundColor Green
    Write-Host "   Message: $($response.message)" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: Organization Test Public" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
}

# Test 10: User Service Health
Write-Host "`nTesting User Service Health..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8083/user/actuator/health"
    Write-Host "SUCCESS: User Service Health" -ForegroundColor Green
    Write-Host "   Status: $($response.status)" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: User Service Health" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
}

# Test 11: Auth Service Health
Write-Host "`nTesting Auth Service Health..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/auth/actuator/health"
    Write-Host "SUCCESS: Auth Service Health" -ForegroundColor Green
    Write-Host "   Status: $($response.status)" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: Auth Service Health" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
}

# Test 12: Organization Service Health
Write-Host "`nTesting Organization Service Health..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8082/organization/actuator/health"
    Write-Host "SUCCESS: Organization Service Health" -ForegroundColor Green
    Write-Host "   Status: $($response.status)" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: Organization Service Health" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
}

Write-Host "`nTesting Complete!" -ForegroundColor Blue
Write-Host "Review the results above to identify any failing endpoints." -ForegroundColor Blue
