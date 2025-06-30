# Test Entity Admin Creation
Write-Host "=== Entity Admin Creation Test ===" -ForegroundColor Yellow

# Step 1: Get SuperAdmin token
Write-Host "Step 1: Getting SuperAdmin token..." -ForegroundColor Cyan
try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/auth/super/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"superadmin","password":"admin123"}'

    if ($loginResponse -and $loginResponse.jwt) {
        $token = $loginResponse.jwt
        Write-Host "✅ SuperAdmin login successful" -ForegroundColor Green
        Write-Host "Token: $($token.Substring(0,50))..." -ForegroundColor Gray
    } else {
        Write-Host "❌ SuperAdmin login failed: No token received" -ForegroundColor Red
        Write-Host "Response: $($loginResponse | ConvertTo-Json)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ SuperAdmin login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Get existing organizations
Write-Host "`nStep 2: Getting existing organizations..." -ForegroundColor Cyan
try {
    $headers = @{ "Authorization" = "Bearer $token" }
    $orgsResponse = Invoke-RestMethod -Uri "http://localhost:8082/organization/super/organizations" -Method GET -Headers $headers
    
    if ($orgsResponse.organizations.Count -gt 0) {
        $orgId = $orgsResponse.organizations[0].id
        $orgName = $orgsResponse.organizations[0].name
        Write-Host "✅ Found organization: $orgName (ID: $orgId)" -ForegroundColor Green
    } else {
        Write-Host "❌ No organizations found" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Failed to get organizations: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Test Entity Admin Creation
Write-Host "`nStep 3: Creating Entity Admin..." -ForegroundColor Cyan

$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$entityAdminData = @{
    username = "entityadmin_$timestamp"
    password = "admin123"
    email = "entityadmin_$timestamp@example.com"
    firstName = "Entity"
    lastName = "Admin"
    mobileNumber = "1234567890"
    organizationId = $orgId
} | ConvertTo-Json

Write-Host "Creating Entity Admin for organization: $orgName (ID: $orgId)" -ForegroundColor Gray
Write-Host "Entity Admin username: entityadmin_$timestamp" -ForegroundColor Gray

try {
    $headers = @{ 
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    }
    
    $response = Invoke-RestMethod -Uri "http://localhost:8082/organization/super/entity-admins" -Method POST -Headers $headers -Body $entityAdminData
    
    Write-Host "✅ Entity Admin creation: SUCCESS" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Gray
    
} catch {
    Write-Host "❌ Entity Admin creation: FAILED" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error details: $errorBody" -ForegroundColor Red
    }
}

Write-Host "`n=== Entity Admin Creation Test Completed ===" -ForegroundColor Yellow
