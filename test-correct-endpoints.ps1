# Test with correct endpoints and credentials
Write-Host "Testing with Correct Endpoints and Credentials" -ForegroundColor Cyan

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

# Test 1: Super Admin Login Direct (Correct Path)
Write-Host "`n=== TESTING SUPER ADMIN LOGIN DIRECT (CORRECT PATH) ===" -ForegroundColor Magenta
$superAdminBody = '{"username":"superadmin","password":"0000"}'
$superAdminResponse = Test-Endpoint -Name "Super Admin Login Direct" -Url "http://localhost:8081/super/auth/login" -Method "POST" -Body $superAdminBody

# Test 2: Super Admin Login Through Gateway (Should work now)
Write-Host "`n=== TESTING SUPER ADMIN LOGIN THROUGH GATEWAY ===" -ForegroundColor Magenta
$superAdminGatewayResponse = Test-Endpoint -Name "Super Admin Login Gateway" -Url "http://localhost:8080/api/auth/super/login" -Method "POST" -Body $superAdminBody

# Test 3: Entity Admin Login Direct (Correct Path)
Write-Host "`n=== TESTING ENTITY ADMIN LOGIN DIRECT (CORRECT PATH) ===" -ForegroundColor Magenta
$entityAdminBody = '{"username":"admin","password":"0000"}'
$entityAdminResponse = Test-Endpoint -Name "Entity Admin Login Direct" -Url "http://localhost:8081/api/auth/login" -Method "POST" -Body $entityAdminBody

# Test 4: Entity Admin Login Through Gateway
Write-Host "`n=== TESTING ENTITY ADMIN LOGIN THROUGH GATEWAY ===" -ForegroundColor Magenta
$entityAdminGatewayResponse = Test-Endpoint -Name "Entity Admin Login Gateway" -Url "http://localhost:8080/api/auth/login" -Method "POST" -Body $entityAdminBody

# Test 5: Subscriber Login Direct (Correct Path) - Using mobile number
Write-Host "`n=== TESTING SUBSCRIBER LOGIN DIRECT (CORRECT PATH) ===" -ForegroundColor Magenta
$subscriberBody = '{"mobileNumber":"1234567890","pin":"0000"}'
$subscriberResponse = Test-Endpoint -Name "Subscriber Login Direct" -Url "http://localhost:8081/api/subscriber/auth/login" -Method "POST" -Body $subscriberBody

# Test 6: Subscriber Login Through Gateway
Write-Host "`n=== TESTING SUBSCRIBER LOGIN THROUGH GATEWAY ===" -ForegroundColor Magenta
$subscriberGatewayResponse = Test-Endpoint -Name "Subscriber Login Gateway" -Url "http://localhost:8080/api/auth/subscriber/login" -Method "POST" -Body $subscriberBody

Write-Host "`n=== TESTING COMPLETE ===" -ForegroundColor Green
