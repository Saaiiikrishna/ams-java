# Test Modern Auth v2 Login
Write-Host "=== Testing Modern Auth v2 Login ===" -ForegroundColor Cyan

$username = "Anu"
$password = "admin123"

$loginData = @{
    username = $username
    password = $password
} | ConvertTo-Json

Write-Host "Login Data: $loginData" -ForegroundColor Yellow

# Test Modern Auth v2 Login
Write-Host "`nTesting Modern Auth v2 login..." -ForegroundColor Cyan
try {
    $modernResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/v2/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SUCCESS: Modern Auth v2 login" -ForegroundColor Green
    Write-Host "Response: $($modernResponse | ConvertTo-Json -Depth 3)" -ForegroundColor Yellow
    
    # Test token validation
    if ($modernResponse.accessToken) {
        Write-Host "`nTesting token validation..." -ForegroundColor Cyan
        try {
            $headers = @{ "Authorization" = "Bearer $($modernResponse.accessToken)" }
            $validateResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/v2/validate" -Method POST -Headers $headers
            Write-Host "✅ SUCCESS: Token validation" -ForegroundColor Green
            Write-Host "Validation Response: $($validateResponse | ConvertTo-Json)" -ForegroundColor Yellow
        } catch {
            Write-Host "❌ FAILED: Token validation" -ForegroundColor Red
            Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
} catch {
    Write-Host "❌ FAILED: Modern Auth v2 login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
    }
}

Write-Host "`n=== Modern Auth Test Complete ===" -ForegroundColor Green
