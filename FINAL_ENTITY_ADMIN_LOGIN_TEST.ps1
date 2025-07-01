# FINAL COMPREHENSIVE ENTITY ADMIN LOGIN TEST
# Tests all EntityAdmin login methods and provides complete status

Write-Host "🎯 FINAL ENTITY ADMIN LOGIN COMPREHENSIVE TEST" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Cyan

$username = "Anu"
$password = "admin123"

$loginData = @{
    username = $username
    password = $password
} | ConvertTo-Json

Write-Host "`n📋 Test Configuration:" -ForegroundColor Yellow
Write-Host "Username: $username"
Write-Host "Password: $password"
Write-Host "Login Data: $loginData"

Write-Host "`n" + "=" * 60 -ForegroundColor Cyan

# Test 1: Traditional EntityAdmin Login (Direct)
Write-Host "`n🔐 TEST 1: Traditional EntityAdmin Login (Direct Auth Service)" -ForegroundColor Cyan
Write-Host "Endpoint: http://localhost:8081/auth/api/auth/login"
try {
    $directResponse = Invoke-RestMethod -Uri "http://localhost:8081/auth/api/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SUCCESS: Traditional Direct Login" -ForegroundColor Green
    Write-Host "JWT Token: $($directResponse.jwt.Substring(0, 50))..." -ForegroundColor Yellow
    $test1Status = "✅ WORKING"
} catch {
    Write-Host "❌ FAILED: Traditional Direct Login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    $test1Status = "❌ FAILED"
}

# Test 2: Traditional EntityAdmin Login (API Gateway)
Write-Host "`n🌐 TEST 2: Traditional EntityAdmin Login (API Gateway)" -ForegroundColor Cyan
Write-Host "Endpoint: http://localhost:8080/api/auth/login"
try {
    $gatewayResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SUCCESS: Traditional Gateway Login" -ForegroundColor Green
    Write-Host "JWT Token: $($gatewayResponse.jwt.Substring(0, 50))..." -ForegroundColor Yellow
    $test2Status = "✅ WORKING"
} catch {
    Write-Host "❌ FAILED: Traditional Gateway Login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    $test2Status = "❌ FAILED"
}

# Test 3: Modern Auth v2 Login (Direct)
Write-Host "`n🚀 TEST 3: Modern Auth v2 Login (Direct Auth Service)" -ForegroundColor Cyan
Write-Host "Endpoint: http://localhost:8081/auth/api/v2/auth/login"
try {
    $modernDirectResponse = Invoke-RestMethod -Uri "http://localhost:8081/auth/api/v2/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SUCCESS: Modern Auth v2 Direct Login" -ForegroundColor Green
    Write-Host "Access Token: $($modernDirectResponse.accessToken.Substring(0, 50))..." -ForegroundColor Yellow
    Write-Host "User Type: $($modernDirectResponse.userType)" -ForegroundColor Yellow
    $test3Status = "✅ WORKING"
} catch {
    Write-Host "❌ FAILED: Modern Auth v2 Direct Login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    $test3Status = "❌ FAILED"
}

# Test 4: Modern Auth v2 Login (API Gateway)
Write-Host "`n🌐🚀 TEST 4: Modern Auth v2 Login (API Gateway)" -ForegroundColor Cyan
Write-Host "Endpoint: http://localhost:8080/api/auth/v2/login"
try {
    $modernGatewayResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/v2/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "✅ SUCCESS: Modern Auth v2 Gateway Login" -ForegroundColor Green
    Write-Host "Access Token: $($modernGatewayResponse.accessToken.Substring(0, 50))..." -ForegroundColor Yellow
    Write-Host "User Type: $($modernGatewayResponse.userType)" -ForegroundColor Yellow
    $test4Status = "✅ WORKING"
} catch {
    Write-Host "❌ FAILED: Modern Auth v2 Gateway Login" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    $test4Status = "❌ FAILED"
}

# Summary
Write-Host "`n" + "=" * 60 -ForegroundColor Cyan
Write-Host "📊 FINAL TEST RESULTS SUMMARY" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Cyan

Write-Host "`n🔐 Traditional EntityAdmin Authentication:" -ForegroundColor Yellow
Write-Host "   Direct Access:     $test1Status"
Write-Host "   API Gateway:       $test2Status"

Write-Host "`n🚀 Modern Auth v2 Authentication:" -ForegroundColor Yellow
Write-Host "   Direct Access:     $test3Status"
Write-Host "   API Gateway:       $test4Status"

# Overall Status
$workingCount = 0
if ($test1Status -eq "✅ WORKING") { $workingCount++ }
if ($test2Status -eq "✅ WORKING") { $workingCount++ }
if ($test3Status -eq "✅ WORKING") { $workingCount++ }
if ($test4Status -eq "✅ WORKING") { $workingCount++ }

Write-Host "`n🎯 OVERALL STATUS: $workingCount/4 Authentication Methods Working" -ForegroundColor Cyan

if ($workingCount -eq 4) {
    Write-Host "🎉 PERFECT! All EntityAdmin login methods are working!" -ForegroundColor Green
} elseif ($workingCount -ge 2) {
    Write-Host "✅ GOOD! Most EntityAdmin login methods are working!" -ForegroundColor Green
} else {
    Write-Host "⚠️ NEEDS WORK: Some EntityAdmin login methods need fixing!" -ForegroundColor Yellow
}

Write-Host "`n" + "=" * 60 -ForegroundColor Cyan
Write-Host "🏁 ENTITY ADMIN LOGIN TEST COMPLETE" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Cyan
