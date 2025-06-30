# Simple Authentication Testing Script
Write-Host "Testing Microservices Authentication" -ForegroundColor Cyan

$API_GATEWAY = "http://localhost:8080"
$AUTH_DIRECT = "http://localhost:8081"

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [string]$Body = $null,
        [hashtable]$Headers = @{}
    )
    
    Write-Host "`nTesting: $Name" -ForegroundColor Yellow
    Write-Host "URL: $Method $Url" -ForegroundColor Gray
    
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
        Write-Host "SUCCESS - Status: $($response.StatusCode)" -ForegroundColor Green
        Write-Host "Response: $($response.Content)" -ForegroundColor Cyan
        return $response
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "Status: $statusCode" -ForegroundColor Yellow
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# Test 1: API Gateway Basic Test
Write-Host "`n=== TESTING API GATEWAY ===" -ForegroundColor Magenta
Test-Endpoint -Name "API Gateway Root" -Url "$API_GATEWAY/"

# Test 2: Auth Service Direct Test
Write-Host "`n=== TESTING AUTH SERVICE DIRECT ===" -ForegroundColor Magenta
Test-Endpoint -Name "Auth Service Test" -Url "$AUTH_DIRECT/auth/test"

# Test 3: Super Admin Login Direct
Write-Host "`n=== TESTING SUPER ADMIN LOGIN DIRECT ===" -ForegroundColor Magenta
$superAdminBody = '{"username":"superadmin","password":"0000"}'
$superAdminResponse = Test-Endpoint -Name "Super Admin Login Direct" -Url "$AUTH_DIRECT/auth/super/login" -Method "POST" -Body $superAdminBody

# Test 4: Super Admin Login Through Gateway
Write-Host "`n=== TESTING SUPER ADMIN LOGIN THROUGH GATEWAY ===" -ForegroundColor Magenta
$superAdminGatewayResponse = Test-Endpoint -Name "Super Admin Login Gateway" -Url "$API_GATEWAY/api/auth/super/login" -Method "POST" -Body $superAdminBody

# Test 5: Entity Admin Login Direct
Write-Host "`n=== TESTING ENTITY ADMIN LOGIN DIRECT ===" -ForegroundColor Magenta
$entityAdminBody = '{"username":"admin@test.com","password":"0000"}'
$entityAdminResponse = Test-Endpoint -Name "Entity Admin Login Direct" -Url "$AUTH_DIRECT/auth/login" -Method "POST" -Body $entityAdminBody

# Test 6: Entity Admin Login Through Gateway
Write-Host "`n=== TESTING ENTITY ADMIN LOGIN THROUGH GATEWAY ===" -ForegroundColor Magenta
$entityAdminGatewayResponse = Test-Endpoint -Name "Entity Admin Login Gateway" -Url "$API_GATEWAY/api/auth/login" -Method "POST" -Body $entityAdminBody

# Test 7: Subscriber Login Direct
Write-Host "`n=== TESTING SUBSCRIBER LOGIN DIRECT ===" -ForegroundColor Magenta
$subscriberBody = '{"username":"subscriber@test.com","password":"0000"}'
$subscriberResponse = Test-Endpoint -Name "Subscriber Login Direct" -Url "$AUTH_DIRECT/auth/subscriber/login" -Method "POST" -Body $subscriberBody

# Test 8: Subscriber Login Through Gateway
Write-Host "`n=== TESTING SUBSCRIBER LOGIN THROUGH GATEWAY ===" -ForegroundColor Magenta
$subscriberGatewayResponse = Test-Endpoint -Name "Subscriber Login Gateway" -Url "$API_GATEWAY/api/auth/subscriber/login" -Method "POST" -Body $subscriberGatewayResponse

Write-Host "`n=== TESTING COMPLETE ===" -ForegroundColor Green
