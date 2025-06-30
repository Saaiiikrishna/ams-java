# Test Organization Service with JWT Token
Write-Host "Testing Organization Service..." -ForegroundColor Cyan

# Get JWT token from auth service
Write-Host "Getting JWT token..." -ForegroundColor Yellow
$response = Invoke-WebRequest -Uri 'http://localhost:8081/auth/super/auth/login' -Method POST -InFile 'test-login.json' -ContentType 'application/json' -UseBasicParsing
$json = $response.Content | ConvertFrom-Json
$token = $json.jwt
Write-Host "JWT Token obtained: $($token.Substring(0,20))..." -ForegroundColor Green

# Test organization service health
Write-Host "`nTesting organization service health..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-WebRequest -Uri 'http://localhost:8082/organization/actuator/health' -UseBasicParsing
    Write-Host "Health Status: $($healthResponse.StatusCode)" -ForegroundColor Green
    Write-Host "Health Response: $($healthResponse.Content)" -ForegroundColor Gray
} catch {
    Write-Host "Health check failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test super admin endpoints with JWT token
$headers = @{ "Authorization" = "Bearer $token" }

Write-Host "`nTesting Super Admin endpoints..." -ForegroundColor Yellow

# Test GET /super/entities
Write-Host "Testing GET /super/entities..." -ForegroundColor Cyan
try {
    $entitiesResponse = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/entities' -Headers $headers -UseBasicParsing
    Write-Host "GET /super/entities: SUCCESS ($($entitiesResponse.StatusCode))" -ForegroundColor Green
    Write-Host "Response: $($entitiesResponse.Content)" -ForegroundColor Gray
} catch {
    Write-Host "GET /super/entities: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Test POST /super/entities (Create organization)
Write-Host "`nTesting POST /super/entities..." -ForegroundColor Cyan
$createOrgBody = @{
    name = "Test Organization"
    description = "Test organization created during testing"
    contactEmail = "test@example.com"
    contactPhone = "1234567890"
    address = "123 Test Street"
} | ConvertTo-Json

try {
    $createResponse = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/entities' -Method POST -Headers $headers -Body $createOrgBody -ContentType 'application/json' -UseBasicParsing
    Write-Host "POST /super/entities: SUCCESS ($($createResponse.StatusCode))" -ForegroundColor Green
    Write-Host "Response: $($createResponse.Content)" -ForegroundColor Gray
} catch {
    Write-Host "POST /super/entities: FAILED - $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $errorResponse = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorResponse)
        $errorContent = $reader.ReadToEnd()
        Write-Host "Error details: $errorContent" -ForegroundColor Red
    }
}

# Test GET /super/permissions
Write-Host "`nTesting GET /super/permissions..." -ForegroundColor Cyan
try {
    $permissionsResponse = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/permissions' -Headers $headers -UseBasicParsing
    Write-Host "GET /super/permissions: SUCCESS ($($permissionsResponse.StatusCode))" -ForegroundColor Green
    Write-Host "Response: $($permissionsResponse.Content)" -ForegroundColor Gray
} catch {
    Write-Host "GET /super/permissions: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nOrganization Service Testing Complete!" -ForegroundColor Cyan
