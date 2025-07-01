# Test Direct Modern Auth v2 Login (bypassing API Gateway)
Write-Host "=== Testing Direct Modern Auth v2 Login ===" -ForegroundColor Cyan

$username = "Anu"
$password = "admin123"

$loginData = @{
    username = $username
    password = $password
} | ConvertTo-Json

Write-Host "Login Data: $loginData" -ForegroundColor Yellow

# Test Direct Modern Auth v2 Login
Write-Host "`nTesting direct Modern Auth v2 login..." -ForegroundColor Cyan
try {
    $directResponse = Invoke-RestMethod -Uri "http://localhost:8081/auth/api/v2/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SUCCESS: Direct Modern Auth v2 login" -ForegroundColor Green
    Write-Host "Response: $($directResponse | ConvertTo-Json -Depth 3)" -ForegroundColor Yellow
} catch {
    Write-Host "❌ FAILED: Direct Modern Auth v2 login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
    }
}

Write-Host "`n=== Direct Modern Auth Test Complete ===" -ForegroundColor Green
