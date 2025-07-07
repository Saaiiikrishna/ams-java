# Test Super Admin Creation

Write-Host "Testing Super Admin Creation..." -ForegroundColor Yellow

# Get SuperAdmin token first
$body = @{
    username = "superadmin"
    password = "admin123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/super/login" -Method Post -Body $body -ContentType "application/json"
    $token = $response.jwt
    Write-Host "Token received successfully" -ForegroundColor Green
} catch {
    Write-Host "Failed to get token: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test Create Super Admin
$authHeader = "Bearer $token"
$headers = @{ 
    "Authorization" = $authHeader
    "X-User-ID" = "1"
}

$timestamp = Get-Date -Format "HHmmss"
$superAdminBody = @{
    username = "testsuperadmin$timestamp"
    password = "admin123"
    email = "testsuperadmin@example.com"
    firstName = "Test"
    lastName = "SuperAdmin"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8083/user/api/users/super-admin" -Method Post -Headers $headers -Body $superAdminBody -ContentType "application/json"
    Write-Host "SUCCESS: Create Super Admin" -ForegroundColor Green
    Write-Host "   Super Admin created successfully" -ForegroundColor Gray
    Write-Host "   Username: testsuperadmin$timestamp" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: Create Super Admin" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
    
    # Try to get more details
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "   Status Code: $statusCode" -ForegroundColor Gray
    }
}
