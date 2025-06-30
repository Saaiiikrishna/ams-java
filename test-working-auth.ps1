# Test with correct passwords
Write-Host "Testing with Correct Passwords" -ForegroundColor Cyan

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

# Test 1: Super Admin Login Direct (Correct Password)
Write-Host "`n=== TESTING SUPER ADMIN LOGIN DIRECT (CORRECT PASSWORD) ===" -ForegroundColor Magenta
$superAdminBody = '{"username":"superadmin","password":"password"}'
$superAdminResponse = Test-Endpoint -Name "Super Admin Login Direct" -Url "http://localhost:8081/super/auth/login" -Method "POST" -Body $superAdminBody

# Test 2: Super Admin Login Through Gateway
Write-Host "`n=== TESTING SUPER ADMIN LOGIN THROUGH GATEWAY ===" -ForegroundColor Magenta
$superAdminGatewayResponse = Test-Endpoint -Name "Super Admin Login Gateway" -Url "http://localhost:8080/api/auth/super/login" -Method "POST" -Body $superAdminBody

# Test 3: Entity Admin Login Direct (Try different passwords)
Write-Host "`n=== TESTING ENTITY ADMIN LOGIN DIRECT ===" -ForegroundColor Magenta
$entityAdminBody1 = '{"username":"admin","password":"password"}'
$entityAdminResponse1 = Test-Endpoint -Name "Entity Admin Login Direct (password)" -Url "http://localhost:8081/api/auth/login" -Method "POST" -Body $entityAdminBody1

$entityAdminBody2 = '{"username":"admin","password":"0000"}'
$entityAdminResponse2 = Test-Endpoint -Name "Entity Admin Login Direct (0000)" -Url "http://localhost:8081/api/auth/login" -Method "POST" -Body $entityAdminBody2

# Test 4: Entity Admin Login Through Gateway
Write-Host "`n=== TESTING ENTITY ADMIN LOGIN THROUGH GATEWAY ===" -ForegroundColor Magenta
$entityAdminGatewayResponse = Test-Endpoint -Name "Entity Admin Login Gateway" -Url "http://localhost:8080/api/auth/login" -Method "POST" -Body $entityAdminBody1

Write-Host "`n=== TESTING COMPLETE ===" -ForegroundColor Green
