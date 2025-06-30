# Test with correct context path /auth
Write-Host "Testing with Correct Context Path" -ForegroundColor Cyan

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

# Test 1: Super Admin Login Direct (With /auth context path)
Write-Host "`n=== TESTING SUPER ADMIN LOGIN DIRECT (WITH CONTEXT PATH) ===" -ForegroundColor Magenta
$superAdminBody = '{"username":"superadmin","password":"password"}'
$superAdminResponse = Test-Endpoint -Name "Super Admin Login Direct" -Url "http://localhost:8081/auth/super/auth/login" -Method "POST" -Body $superAdminBody

# Test 2: Entity Admin Login Direct (With /auth context path)
Write-Host "`n=== TESTING ENTITY ADMIN LOGIN DIRECT (WITH CONTEXT PATH) ===" -ForegroundColor Magenta
$entityAdminBody = '{"username":"admin","password":"password"}'
$entityAdminResponse = Test-Endpoint -Name "Entity Admin Login Direct" -Url "http://localhost:8081/auth/api/auth/login" -Method "POST" -Body $entityAdminBody

# Test 3: Test what we saw in logs - the working endpoint
Write-Host "`n=== TESTING ENDPOINT FROM LOGS ===" -ForegroundColor Magenta
$superAdminResponse2 = Test-Endpoint -Name "Super Admin Login (From Logs)" -Url "http://localhost:8081/auth/super/auth/login" -Method "POST" -Body $superAdminBody

Write-Host "`n=== TESTING COMPLETE ===" -ForegroundColor Green
