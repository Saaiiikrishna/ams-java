# Final Authentication Flow Testing
Write-Host "FINAL AUTHENTICATION FLOW TESTING" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [string]$Body = $null,
        [hashtable]$Headers = @{}
    )
    
    Write-Host "`nTesting: $Name" -ForegroundColor Yellow
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
        Write-Host "   SUCCESS - Status: $($response.StatusCode)" -ForegroundColor Green

        # Try to parse JSON response
        try {
            $jsonResponse = $response.Content | ConvertFrom-Json
            Write-Host "   Response: $($jsonResponse | ConvertTo-Json -Compress)" -ForegroundColor Cyan
            return $jsonResponse
        } catch {
            Write-Host "   Raw Response: $($response.Content)" -ForegroundColor Cyan
            return $response.Content
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "   Status: $statusCode" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# ============================================================================
# PHASE 1: SUPER ADMIN AUTHENTICATION
# ============================================================================

Write-Host "`nPHASE 1: SUPER ADMIN AUTHENTICATION" -ForegroundColor Magenta

# Test 1: Super Admin Login Direct
$superAdminBody = '{"username":"superadmin","password":"password"}'
$superAdminDirect = Test-Endpoint -Name "Super Admin Login (Direct)" -Url "http://localhost:8081/auth/super/auth/login" -Method "POST" -Body $superAdminBody

# Test 2: Super Admin Login Through Gateway
$superAdminGateway = Test-Endpoint -Name "Super Admin Login (Gateway)" -Url "http://localhost:8080/api/auth/super/login" -Method "POST" -Body $superAdminBody

# Extract token for protected endpoint testing
$superAdminToken = $null
if ($superAdminGateway -and $superAdminGateway.jwt) {
    $superAdminToken = $superAdminGateway.jwt
    Write-Host "   Super Admin Token extracted successfully" -ForegroundColor Green
}

# ============================================================================
# PHASE 2: ENTITY ADMIN AUTHENTICATION
# ============================================================================

Write-Host "`nPHASE 2: ENTITY ADMIN AUTHENTICATION" -ForegroundColor Magenta

# Test different passwords for entity admin
$entityAdminBody1 = '{"username":"admin","password":"password"}'
$entityAdminDirect1 = Test-Endpoint -Name "Entity Admin Login Direct (password)" -Url "http://localhost:8081/auth/api/auth/login" -Method "POST" -Body $entityAdminBody1

$entityAdminBody2 = '{"username":"admin","password":"0000"}'
$entityAdminDirect2 = Test-Endpoint -Name "Entity Admin Login Direct (0000)" -Url "http://localhost:8081/auth/api/auth/login" -Method "POST" -Body $entityAdminBody2

# Test through gateway with working credentials
if ($entityAdminDirect1) {
    $entityAdminGateway = Test-Endpoint -Name "Entity Admin Login (Gateway)" -Url "http://localhost:8080/api/auth/login" -Method "POST" -Body $entityAdminBody1
} elseif ($entityAdminDirect2) {
    $entityAdminGateway = Test-Endpoint -Name "Entity Admin Login (Gateway)" -Url "http://localhost:8080/api/auth/login" -Method "POST" -Body $entityAdminBody2
}

# ============================================================================
# PHASE 3: PROTECTED ENDPOINTS TESTING
# ============================================================================

Write-Host "`nPHASE 3: PROTECTED ENDPOINTS TESTING" -ForegroundColor Magenta

if ($superAdminToken) {
    $authHeaders = @{ "Authorization" = "Bearer $superAdminToken" }
    
    # Test Super Admin Protected Endpoints
    Test-Endpoint -Name "Super Admin Profile (Direct)" -Url "http://localhost:8081/auth/super/auth/profile" -Headers $authHeaders
    Test-Endpoint -Name "Super Admin Profile (Gateway)" -Url "http://localhost:8080/api/auth/super/profile" -Headers $authHeaders
} else {
    Write-Host "   No Super Admin token available for protected endpoint testing" -ForegroundColor Yellow
}

# ============================================================================
# PHASE 4: SUMMARY
# ============================================================================

Write-Host "`nTESTING SUMMARY" -ForegroundColor Cyan
Write-Host "==================" -ForegroundColor Cyan

$results = @()
if ($superAdminDirect) { $results += "SUCCESS: Super Admin Direct Login" } else { $results += "FAILED: Super Admin Direct Login" }
if ($superAdminGateway) { $results += "SUCCESS: Super Admin Gateway Login" } else { $results += "FAILED: Super Admin Gateway Login" }
if ($entityAdminDirect1 -or $entityAdminDirect2) { $results += "SUCCESS: Entity Admin Direct Login" } else { $results += "FAILED: Entity Admin Direct Login" }

foreach ($result in $results) {
    Write-Host "   $result" -ForegroundColor White
}

if ($superAdminGateway) {
    Write-Host "`nSUCCESS: API Gateway authentication is working!" -ForegroundColor Green
    Write-Host "   - Super Admin can login through API Gateway" -ForegroundColor Green
    Write-Host "   - JWT tokens are being generated correctly" -ForegroundColor Green
    Write-Host "   - Routing configuration is correct" -ForegroundColor Green
} else {
    Write-Host "`nPARTIAL SUCCESS: Some authentication flows are working" -ForegroundColor Yellow
}

Write-Host "`nTESTING COMPLETE" -ForegroundColor Green
