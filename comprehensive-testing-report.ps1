# Comprehensive Testing Report for AMS Microservices
# Testing all implemented services and documenting results

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "COMPREHENSIVE AMS MICROSERVICES TESTING" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$testResults = @()
$startTime = Get-Date

function Test-Service {
    param(
        [string]$ServiceName,
        [string]$Url,
        [string]$Method = "GET",
        [string]$Body = $null,
        [hashtable]$Headers = @{},
        [string]$ExpectedStatus = "200"
    )
    
    Write-Host "`nTesting: $ServiceName" -ForegroundColor Yellow
    Write-Host "URL: $Method $Url" -ForegroundColor Gray
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $Headers
            UseBasicParsing = $true
            TimeoutSec = 10
        }
        
        if ($Body) {
            $params.Body = $Body
            $params.ContentType = "application/json"
        }
        
        $response = Invoke-WebRequest @params
        $success = $response.StatusCode -eq $ExpectedStatus
        
        $result = @{
            Service = $ServiceName
            URL = $Url
            Method = $Method
            Status = $response.StatusCode
            Success = $success
            ResponseTime = (Get-Date) - $testStart
            Error = $null
        }
        
        if ($success) {
            Write-Host "SUCCESS - Status: $($response.StatusCode)" -ForegroundColor Green
        } else {
            Write-Host "UNEXPECTED STATUS - Expected: $ExpectedStatus, Got: $($response.StatusCode)" -ForegroundColor Yellow
        }
        
        return $result
    } catch {
        $result = @{
            Service = $ServiceName
            URL = $Url
            Method = $Method
            Status = "ERROR"
            Success = $false
            ResponseTime = (Get-Date) - $testStart
            Error = $_.Exception.Message
        }
        
        Write-Host "FAILED - Error: $($_.Exception.Message)" -ForegroundColor Red
        return $result
    }
}

# ============================================================================
# PHASE 1: INFRASTRUCTURE TESTING
# ============================================================================

Write-Host "`n=== PHASE 1: INFRASTRUCTURE TESTING ===" -ForegroundColor Magenta

# Test Database Connectivity
Write-Host "`nTesting Database Connectivity..." -ForegroundColor Cyan
try {
    $dbTest = docker exec ams-postgres pg_isready -U postgres
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Database: HEALTHY" -ForegroundColor Green
        $testResults += @{Service="PostgreSQL Database"; Status="HEALTHY"; Success=$true}
    } else {
        Write-Host "Database: UNHEALTHY" -ForegroundColor Red
        $testResults += @{Service="PostgreSQL Database"; Status="UNHEALTHY"; Success=$false}
    }
} catch {
    Write-Host "Database: ERROR - $($_.Exception.Message)" -ForegroundColor Red
    $testResults += @{Service="PostgreSQL Database"; Status="ERROR"; Success=$false; Error=$_.Exception.Message}
}

# Test Redis Connectivity
Write-Host "`nTesting Redis Connectivity..." -ForegroundColor Cyan
try {
    $redisTest = docker exec ams-redis redis-cli ping
    if ($redisTest -eq "PONG") {
        Write-Host "Redis: HEALTHY" -ForegroundColor Green
        $testResults += @{Service="Redis Cache"; Status="HEALTHY"; Success=$true}
    } else {
        Write-Host "Redis: UNHEALTHY" -ForegroundColor Red
        $testResults += @{Service="Redis Cache"; Status="UNHEALTHY"; Success=$false}
    }
} catch {
    Write-Host "Redis: ERROR - $($_.Exception.Message)" -ForegroundColor Red
    $testResults += @{Service="Redis Cache"; Status="ERROR"; Success=$false; Error=$_.Exception.Message}
}

# ============================================================================
# PHASE 2: MICROSERVICES HEALTH CHECKS
# ============================================================================

Write-Host "`n=== PHASE 2: MICROSERVICES HEALTH CHECKS ===" -ForegroundColor Magenta

# Test Auth Service Health
$testStart = Get-Date
$authHealthResult = Test-Service -ServiceName "Auth Service Health" -Url "http://localhost:8081/auth/actuator/health"
$testResults += $authHealthResult

# Test API Gateway Health
$testStart = Get-Date
$gatewayHealthResult = Test-Service -ServiceName "API Gateway Health" -Url "http://localhost:8080/actuator/health"
$testResults += $gatewayHealthResult

# ============================================================================
# PHASE 3: AUTHENTICATION TESTING
# ============================================================================

Write-Host "`n=== PHASE 3: AUTHENTICATION TESTING ===" -ForegroundColor Magenta

# Test Super Admin Login (Direct)
$testStart = Get-Date
$superAdminBody = '{"username":"superadmin","password":"password"}'
$superAdminDirectResult = Test-Service -ServiceName "Super Admin Login (Direct)" -Url "http://localhost:8081/auth/super/auth/login" -Method "POST" -Body $superAdminBody
$testResults += $superAdminDirectResult

# Test Super Admin Login (Gateway)
$testStart = Get-Date
$superAdminGatewayResult = Test-Service -ServiceName "Super Admin Login (Gateway)" -Url "http://localhost:8080/api/auth/super/login" -Method "POST" -Body $superAdminBody
$testResults += $superAdminGatewayResult

# Extract JWT token for further testing
$superAdminToken = $null
if ($superAdminGatewayResult.Success) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/super/login" -Method "POST" -Body $superAdminBody -ContentType "application/json" -UseBasicParsing
        $responseData = $response.Content | ConvertFrom-Json
        if ($responseData.jwt) {
            $superAdminToken = $responseData.jwt
            Write-Host "JWT Token extracted successfully" -ForegroundColor Green
        }
    } catch {
        Write-Host "Failed to extract JWT token" -ForegroundColor Yellow
    }
}

# Test Entity Admin Login
$testStart = Get-Date
$entityAdminBody = '{"username":"admin","password":"password"}'
$entityAdminResult = Test-Service -ServiceName "Entity Admin Login" -Url "http://localhost:8081/auth/api/auth/login" -Method "POST" -Body $entityAdminBody
$testResults += $entityAdminResult

# ============================================================================
# PHASE 4: PROTECTED ENDPOINTS TESTING
# ============================================================================

Write-Host "`n=== PHASE 4: PROTECTED ENDPOINTS TESTING ===" -ForegroundColor Magenta

if ($superAdminToken) {
    $authHeaders = @{ "Authorization" = "Bearer $superAdminToken" }
    
    # Test Super Admin Profile
    $testStart = Get-Date
    $profileResult = Test-Service -ServiceName "Super Admin Profile" -Url "http://localhost:8080/api/auth/super/profile" -Headers $authHeaders
    $testResults += $profileResult
} else {
    Write-Host "Skipping protected endpoint tests - No valid JWT token" -ForegroundColor Yellow
    $testResults += @{Service="Protected Endpoints"; Status="SKIPPED"; Success=$false; Error="No JWT token available"}
}

# ============================================================================
# PHASE 5: SERVICE DISCOVERY TESTING
# ============================================================================

Write-Host "`n=== PHASE 5: SERVICE DISCOVERY TESTING ===" -ForegroundColor Magenta

# Check if services are broadcasting discovery information
Write-Host "Checking service discovery logs..." -ForegroundColor Cyan
try {
    $authLogs = docker logs ams-auth-service --tail 5 2>&1 | Select-String "NetworkDiscoveryService"
    if ($authLogs) {
        Write-Host "Service Discovery: ACTIVE" -ForegroundColor Green
        $testResults += @{Service="Service Discovery"; Status="ACTIVE"; Success=$true}
    } else {
        Write-Host "Service Discovery: NO ACTIVITY DETECTED" -ForegroundColor Yellow
        $testResults += @{Service="Service Discovery"; Status="NO_ACTIVITY"; Success=$false}
    }
} catch {
    Write-Host "Service Discovery: ERROR - $($_.Exception.Message)" -ForegroundColor Red
    $testResults += @{Service="Service Discovery"; Status="ERROR"; Success=$false; Error=$_.Exception.Message}
}

# ============================================================================
# PHASE 6: DOCKER CONTAINER STATUS
# ============================================================================

Write-Host "`n=== PHASE 6: DOCKER CONTAINER STATUS ===" -ForegroundColor Magenta

try {
    $containers = docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | ConvertFrom-String -Delimiter "`t"
    Write-Host "Active Containers:" -ForegroundColor Cyan
    foreach ($container in $containers) {
        if ($container.P1 -like "ams-*") {
            $status = if ($container.P2 -like "*healthy*") { "HEALTHY" } elseif ($container.P2 -like "*Up*") { "RUNNING" } else { "UNKNOWN" }
            Write-Host "  $($container.P1): $status" -ForegroundColor $(if ($status -eq "HEALTHY") { "Green" } elseif ($status -eq "RUNNING") { "Yellow" } else { "Red" })
            $testResults += @{Service=$container.P1; Status=$status; Success=($status -ne "UNKNOWN")}
        }
    }
} catch {
    Write-Host "Error checking container status: $($_.Exception.Message)" -ForegroundColor Red
}

# ============================================================================
# PHASE 7: GENERATE COMPREHENSIVE REPORT
# ============================================================================

Write-Host "`n=== COMPREHENSIVE TESTING REPORT ===" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

$totalTests = $testResults.Count
$successfulTests = ($testResults | Where-Object { $_.Success -eq $true }).Count
$failedTests = $totalTests - $successfulTests
$successRate = [math]::Round(($successfulTests / $totalTests) * 100, 2)

Write-Host "`nOVERALL RESULTS:" -ForegroundColor White
Write-Host "Total Tests: $totalTests" -ForegroundColor White
Write-Host "Successful: $successfulTests" -ForegroundColor Green
Write-Host "Failed: $failedTests" -ForegroundColor Red
Write-Host "Success Rate: $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 60) { "Yellow" } else { "Red" })

Write-Host "`nDETAILED RESULTS:" -ForegroundColor White
foreach ($result in $testResults) {
    $statusColor = if ($result.Success) { "Green" } else { "Red" }
    $status = if ($result.Success) { "PASS" } else { "FAIL" }
    Write-Host "  [$status] $($result.Service)" -ForegroundColor $statusColor
    if ($result.Error) {
        Write-Host "    Error: $($result.Error)" -ForegroundColor Red
    }
}

$endTime = Get-Date
$totalTime = $endTime - $startTime

Write-Host "`nTesting completed in $($totalTime.TotalSeconds) seconds" -ForegroundColor Cyan

# Save results to file
$reportData = @{
    TestDate = $startTime
    TotalTests = $totalTests
    SuccessfulTests = $successfulTests
    FailedTests = $failedTests
    SuccessRate = $successRate
    TestDuration = $totalTime.TotalSeconds
    Results = $testResults
}

$reportData | ConvertTo-Json -Depth 3 | Out-File "testing-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').json"
Write-Host "`nDetailed report saved to testing-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').json" -ForegroundColor Green
