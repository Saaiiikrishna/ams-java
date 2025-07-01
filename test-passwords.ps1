# Test different passwords for SuperAdmin
$passwords = @("admin", "password", "admin123", "superadmin", "0000", "1234", "password123")

foreach ($password in $passwords) {
    Write-Host "Testing password: $password" -ForegroundColor Cyan
    
    $loginData = @{
        username = "superadmin"
        password = $password
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8081/auth/super/auth/login" -Method POST -Body $loginData -ContentType "application/json"
        Write-Host "✅ SUCCESS: Password '$password' works!" -ForegroundColor Green
        Write-Host "Response: $($response | ConvertTo-Json)" -ForegroundColor Yellow
        break
    } catch {
        Write-Host "❌ Failed with password: $password" -ForegroundColor Red
    }
}
