# Test SuperAdmin Login
$loginData = @{
    username = "superadmin"
    password = "superadmin123"
} | ConvertTo-Json

Write-Host "Testing SuperAdmin login..."
Write-Host "Login Data: $loginData"

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/auth/super/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "SUCCESS: SuperAdmin login works!" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json)" -ForegroundColor Yellow
} catch {
    Write-Host "FAILED: SuperAdmin login failed" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}
