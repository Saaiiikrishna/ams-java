# Test EntityAdmin Anu Login with Correct Password
Write-Host "=== Testing EntityAdmin 'Anu' Login ===" -ForegroundColor Cyan

$username = "Anu"
$password = "admin123"  # Using the same password as SuperAdmin

$loginData = @{
    username = $username
    password = $password
} | ConvertTo-Json

Write-Host "Login Data: $loginData" -ForegroundColor Yellow

# Test 1: Direct Auth Service Login
Write-Host "`nTest 1: Testing direct Auth Service login..." -ForegroundColor Cyan
try {
    $directResponse = Invoke-RestMethod -Uri "http://localhost:8081/auth/api/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SUCCESS: Direct Auth Service login" -ForegroundColor Green
    Write-Host "JWT Token: $($directResponse.jwt)" -ForegroundColor Yellow
    $directToken = $directResponse.jwt
} catch {
    Write-Host "❌ FAILED: Direct Auth Service login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: API Gateway Login
Write-Host "`nTest 2: Testing API Gateway login..." -ForegroundColor Cyan
try {
    $gatewayResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SUCCESS: API Gateway login" -ForegroundColor Green
    Write-Host "JWT Token: $($gatewayResponse.jwt)" -ForegroundColor Yellow
    $gatewayToken = $gatewayResponse.jwt
} catch {
    Write-Host "❌ FAILED: API Gateway login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Modern Auth v2 Login
Write-Host "`nTest 3: Testing Modern Auth v2 login..." -ForegroundColor Cyan
try {
    $modernResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/v2/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SUCCESS: Modern Auth v2 login" -ForegroundColor Green
    Write-Host "Access Token: $($modernResponse.accessToken)" -ForegroundColor Yellow
    $modernToken = $modernResponse.accessToken
} catch {
    Write-Host "❌ FAILED: Modern Auth v2 login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Test protected endpoint with tokens
if ($directToken) {
    Write-Host "`nTest 4: Testing protected endpoint with Direct token..." -ForegroundColor Cyan
    try {
        $headers = @{ "Authorization" = "Bearer $directToken" }
        $protectedResponse = Invoke-RestMethod -Uri "http://localhost:8081/auth/api/auth/validate" -Method POST -Headers $headers
        Write-Host "✅ SUCCESS: Protected endpoint with Direct token" -ForegroundColor Green
    } catch {
        Write-Host "❌ FAILED: Protected endpoint with Direct token" -ForegroundColor Red
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

if ($gatewayToken) {
    Write-Host "`nTest 5: Testing protected endpoint with Gateway token..." -ForegroundColor Cyan
    try {
        $headers = @{ "Authorization" = "Bearer $gatewayToken" }
        $protectedResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/validate" -Method POST -Headers $headers
        Write-Host "✅ SUCCESS: Protected endpoint with Gateway token" -ForegroundColor Green
    } catch {
        Write-Host "❌ FAILED: Protected endpoint with Gateway token" -ForegroundColor Red
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n=== EntityAdmin Login Test Complete ===" -ForegroundColor Green
