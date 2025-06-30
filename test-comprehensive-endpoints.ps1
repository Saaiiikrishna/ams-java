# Comprehensive Endpoint Testing Script
# Tests all authentication flows and microservices endpoints

Write-Host "🧪 COMPREHENSIVE MICROSERVICES TESTING" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan

# Configuration
$API_GATEWAY = "http://localhost:8080"
$AUTH_DIRECT = "http://localhost:8081"

# Test Results Storage
$TestResults = @()

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [int]$ExpectedStatus = 200
    )
    
    Write-Host "`n🔍 Testing: $Name" -ForegroundColor Yellow
    Write-Host "   URL: $Method $Url" -ForegroundColor Gray
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $Headers
            UseBasicParsing = $true
        }
        
        if ($Body) {
            $params.Body = $Body
            $params.ContentType = "application/json"
        }
        
        $response = Invoke-WebRequest @params
        $statusCode = $response.StatusCode
        $content = $response.Content
        
        $result = @{
            Name = $Name
            Status = "✅ PASS"
            StatusCode = $statusCode
            Expected = $ExpectedStatus
            Response = $content
            Url = $Url
        }
        
        if ($statusCode -eq $ExpectedStatus) {
            Write-Host "   ✅ PASS - Status: $statusCode" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️  UNEXPECTED - Status: $statusCode (Expected: $ExpectedStatus)" -ForegroundColor Yellow
            $result.Status = "⚠️ UNEXPECTED"
        }
        
        # Try to parse JSON response
        try {
            $jsonResponse = $content | ConvertFrom-Json
            $result.JsonResponse = $jsonResponse
            Write-Host "   📄 JSON Response: $($jsonResponse | ConvertTo-Json -Compress)" -ForegroundColor Cyan
        } catch {
            Write-Host "   📄 Raw Response: $content" -ForegroundColor Cyan
        }
        
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $result = @{
            Name = $Name
            Status = "❌ FAIL"
            StatusCode = $statusCode
            Expected = $ExpectedStatus
            Error = $_.Exception.Message
            Url = $Url
        }
        
        if ($statusCode -eq $ExpectedStatus) {
            Write-Host "   ✅ EXPECTED ERROR - Status: $statusCode" -ForegroundColor Green
            $result.Status = "✅ EXPECTED"
        } else {
            Write-Host "   ❌ FAIL - Status: $statusCode, Error: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    $TestResults += $result
    return $result
}

# ============================================================================
# PHASE 1: INFRASTRUCTURE TESTING
# ============================================================================

Write-Host "`n🏗️  PHASE 1: INFRASTRUCTURE TESTING" -ForegroundColor Magenta

# Test API Gateway Health
Test-Endpoint -Name "API Gateway Health" -Method "GET" -Url "$API_GATEWAY/actuator/health" -ExpectedStatus 404

# Test Auth Service Direct Access
Test-Endpoint -Name "Auth Service Direct Health" -Method "GET" -Url "$AUTH_DIRECT/actuator/health" -ExpectedStatus 404

# Test Auth Service Basic Endpoint
Test-Endpoint -Name "Auth Service Basic Test" -Method "GET" -Url "$AUTH_DIRECT/auth/test" -ExpectedStatus 403

# ============================================================================
# PHASE 2: SUPER ADMIN AUTHENTICATION FLOW
# ============================================================================

Write-Host "`n👑 PHASE 2: SUPER ADMIN AUTHENTICATION FLOW" -ForegroundColor Magenta

# Super Admin Login (Direct to Auth Service)
$superAdminLoginBody = @{
    username = "superadmin"
    password = "0000"
} | ConvertTo-Json

$superAdminLogin = Test-Endpoint -Name "Super Admin Login (Direct)" -Method "POST" -Url "$AUTH_DIRECT/auth/super/login" -Body $superAdminLoginBody -ExpectedStatus 200

# Super Admin Login (Through API Gateway)
$superAdminLoginGateway = Test-Endpoint -Name "Super Admin Login (Gateway)" -Method "POST" -Url "$API_GATEWAY/api/auth/super/login" -Body $superAdminLoginBody -ExpectedStatus 200

# Extract tokens if login successful
$superAdminToken = $null
if ($superAdminLogin.JsonResponse -and $superAdminLogin.JsonResponse.accessToken) {
    $superAdminToken = $superAdminLogin.JsonResponse.accessToken
    Write-Host "   🔑 Super Admin Token: $($superAdminToken.Substring(0, 50))..." -ForegroundColor Green
}

# ============================================================================
# PHASE 3: ENTITY ADMIN AUTHENTICATION FLOW  
# ============================================================================

Write-Host "`n🏢 PHASE 3: ENTITY ADMIN AUTHENTICATION FLOW" -ForegroundColor Magenta

# Entity Admin Login (Direct to Auth Service)
$entityAdminLoginBody = @{
    username = "admin@test.com"
    password = "0000"
} | ConvertTo-Json

$entityAdminLogin = Test-Endpoint -Name "Entity Admin Login (Direct)" -Method "POST" -Url "$AUTH_DIRECT/auth/login" -Body $entityAdminLoginBody -ExpectedStatus 200

# Entity Admin Login (Through API Gateway)
$entityAdminLoginGateway = Test-Endpoint -Name "Entity Admin Login (Gateway)" -Method "POST" -Url "$API_GATEWAY/api/auth/login" -Body $entityAdminLoginBody -ExpectedStatus 200

# Extract tokens if login successful
$entityAdminToken = $null
if ($entityAdminLogin.JsonResponse -and $entityAdminLogin.JsonResponse.accessToken) {
    $entityAdminToken = $entityAdminLogin.JsonResponse.accessToken
    Write-Host "   🔑 Entity Admin Token: $($entityAdminToken.Substring(0, 50))..." -ForegroundColor Green
}

# ============================================================================
# PHASE 4: SUBSCRIBER AUTHENTICATION FLOW
# ============================================================================

Write-Host "`n👤 PHASE 4: SUBSCRIBER AUTHENTICATION FLOW" -ForegroundColor Magenta

# Subscriber Login (Direct to Auth Service)
$subscriberLoginBody = @{
    username = "subscriber@test.com"
    password = "0000"
} | ConvertTo-Json

$subscriberLogin = Test-Endpoint -Name "Subscriber Login (Direct)" -Method "POST" -Url "$AUTH_DIRECT/auth/subscriber/login" -Body $subscriberLoginBody -ExpectedStatus 200

# Subscriber Login (Through API Gateway)
$subscriberLoginGateway = Test-Endpoint -Name "Subscriber Login (Gateway)" -Method "POST" -Url "$API_GATEWAY/api/auth/subscriber/login" -Body $subscriberLoginBody -ExpectedStatus 200

# ============================================================================
# PHASE 5: PROTECTED ENDPOINTS TESTING
# ============================================================================

Write-Host "`n🔒 PHASE 5: PROTECTED ENDPOINTS TESTING" -ForegroundColor Magenta

if ($superAdminToken) {
    $authHeaders = @{ "Authorization" = "Bearer $superAdminToken" }
    
    # Test Super Admin Protected Endpoints
    Test-Endpoint -Name "Super Admin Profile" -Method "GET" -Url "$AUTH_DIRECT/auth/super/profile" -Headers $authHeaders
    Test-Endpoint -Name "Super Admin Profile (Gateway)" -Method "GET" -Url "$API_GATEWAY/api/auth/super/profile" -Headers $authHeaders
}

if ($entityAdminToken) {
    $authHeaders = @{ "Authorization" = "Bearer $entityAdminToken" }
    
    # Test Entity Admin Protected Endpoints
    Test-Endpoint -Name "Entity Admin Profile" -Method "GET" -Url "$AUTH_DIRECT/auth/profile" -Headers $authHeaders
    Test-Endpoint -Name "Entity Admin Profile (Gateway)" -Method "GET" -Url "$API_GATEWAY/api/auth/profile" -Headers $authHeaders
}

# ============================================================================
# RESULTS SUMMARY
# ============================================================================

Write-Host "`n📊 TEST RESULTS SUMMARY" -ForegroundColor Cyan
Write-Host "========================" -ForegroundColor Cyan

$passCount = ($TestResults | Where-Object { $_.Status -like "*PASS*" -or $_.Status -like "*EXPECTED*" }).Count
$failCount = ($TestResults | Where-Object { $_.Status -like "*FAIL*" }).Count
$unexpectedCount = ($TestResults | Where-Object { $_.Status -like "*UNEXPECTED*" }).Count

Write-Host "✅ Passed: $passCount" -ForegroundColor Green
Write-Host "❌ Failed: $failCount" -ForegroundColor Red
Write-Host "⚠️  Unexpected: $unexpectedCount" -ForegroundColor Yellow
Write-Host "📊 Total: $($TestResults.Count)" -ForegroundColor Cyan

# Detailed Results
Write-Host "`n📋 DETAILED RESULTS:" -ForegroundColor Cyan
foreach ($result in $TestResults) {
    Write-Host "   $($result.Status) $($result.Name) - Status: $($result.StatusCode)" -ForegroundColor White
}

# Save results to file
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$TestResults | ConvertTo-Json -Depth 3 | Out-File "test-results-$timestamp.json"
Write-Host "`n💾 Results saved to test-results-$timestamp.json" -ForegroundColor Green
