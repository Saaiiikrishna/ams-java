$response = Invoke-WebRequest -Uri 'http://localhost:8081/auth/super/auth/login' -Method POST -InFile 'test-login.json' -ContentType 'application/json' -UseBasicParsing
$json = $response.Content | ConvertFrom-Json
Write-Host "JWT Token:" -ForegroundColor Green
Write-Host $json.jwt -ForegroundColor Yellow

# Test protected endpoint
$headers = @{ "Authorization" = "Bearer $($json.jwt)" }
Write-Host "`nTesting protected endpoint..." -ForegroundColor Cyan
try {
    $profileResponse = Invoke-WebRequest -Uri 'http://localhost:8081/auth/super/profile' -Headers $headers -UseBasicParsing
    Write-Host "Profile endpoint: SUCCESS" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor White
    Write-Host $profileResponse.Content -ForegroundColor Gray
} catch {
    Write-Host "Profile endpoint: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}
