# Test EntityAdmin Login with Proper JSON Formatting
Write-Host "=== Testing EntityAdmin Login with Fixed JSON ===" -ForegroundColor Cyan

$username = "Anu"
$password = "admin123"

# Create properly formatted JSON
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
    Write-Host "Response: $($directResponse | ConvertTo-Json)" -ForegroundColor Yellow
} catch {
    Write-Host "❌ FAILED: Direct Auth Service login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
    }
}

# Test 2: API Gateway Login
Write-Host "`nTest 2: Testing API Gateway login..." -ForegroundColor Cyan
try {
    $gatewayResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SUCCESS: API Gateway login" -ForegroundColor Green
    Write-Host "Response: $($gatewayResponse | ConvertTo-Json)" -ForegroundColor Yellow
} catch {
    Write-Host "❌ FAILED: API Gateway login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
    }
}

# Test 3: Modern Auth v2 Login
Write-Host "`nTest 3: Testing Modern Auth v2 login..." -ForegroundColor Cyan
try {
    $modernResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/v2/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SUCCESS: Modern Auth v2 login" -ForegroundColor Green
    Write-Host "Response: $($modernResponse | ConvertTo-Json)" -ForegroundColor Yellow
} catch {
    Write-Host "❌ FAILED: Modern Auth v2 login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
    }
}

# Test 4: Test with admin user for comparison
Write-Host "`nTest 4: Testing with admin user for comparison..." -ForegroundColor Cyan
$adminData = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

try {
    $adminResponse = Invoke-RestMethod -Uri "http://localhost:8081/auth/api/auth/login" -Method POST -Body $adminData -ContentType "application/json"
    Write-Host "✅ SUCCESS: Admin login works" -ForegroundColor Green
    Write-Host "Response: $($adminResponse | ConvertTo-Json)" -ForegroundColor Yellow
} catch {
    Write-Host "❌ FAILED: Admin login also fails" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
    }
}

Write-Host "`n=== Test Complete ===" -ForegroundColor Green
