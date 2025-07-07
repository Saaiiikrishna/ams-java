# Comprehensive Microservices Endpoint Testing Script
# Tests all endpoints across auth-service, user-service, organization-service, and attendance-service

param(
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$AuthUrl = "http://localhost:8081",
    [string]$UserUrl = "http://localhost:8083", 
    [string]$OrgUrl = "http://localhost:8082",
    [string]$AttendanceUrl = "http://localhost:8084"
)

# Colors for output
$Green = "Green"
$Red = "Red"
$Yellow = "Yellow"
$Blue = "Blue"

function Write-TestResult {
    param($TestName, $Status, $Details = "")
    $timestamp = Get-Date -Format "HH:mm:ss"
    if ($Status -eq "PASS") {
        Write-Host "[$timestamp] ✅ $TestName" -ForegroundColor $Green
    } elseif ($Status -eq "FAIL") {
        Write-Host "[$timestamp] ❌ $TestName" -ForegroundColor $Red
    } elseif ($Status -eq "SKIP") {
        Write-Host "[$timestamp] ⏭️  $TestName" -ForegroundColor $Yellow
    } else {
        Write-Host "[$timestamp] ℹ️  $TestName" -ForegroundColor $Blue
    }
    if ($Details) {
        Write-Host "    $Details" -ForegroundColor Gray
    }
}

function Test-Endpoint {
    param(
        [string]$Url,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [string]$TestName
    )
    
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
        
        if ($statusCode -ge 200 -and $statusCode -lt 300) {
            Write-TestResult $TestName "PASS" "Status: $statusCode"
            return @{ Success = $true; StatusCode = $statusCode; Content = $response.Content }
        } else {
            Write-TestResult $TestName "FAIL" "Status: $statusCode"
            return @{ Success = $false; StatusCode = $statusCode; Content = $response.Content }
        }
    } catch {
        Write-TestResult $TestName "FAIL" "Error: $($_.Exception.Message)"
        return @{ Success = $false; Error = $_.Exception.Message }
    }
}

# Global variables for tokens
$SuperAdminToken = $null
$EntityAdminToken = $null
$MemberToken = $null

Write-Host "🚀 Starting Comprehensive Microservices Endpoint Testing" -ForegroundColor $Blue
Write-Host "=================================================" -ForegroundColor $Blue

# ========== PHASE 1: AUTHENTICATION TESTS ==========
Write-Host "`n🔐 PHASE 1: Authentication Service Tests" -ForegroundColor $Blue

# Test 1: SuperAdmin Login
Write-TestResult "Testing SuperAdmin Login" "INFO"
$loginBody = @{
    username = "superadmin"
    password = "admin123"
} | ConvertTo-Json

$loginResult = Test-Endpoint -Url "$GatewayUrl/api/auth/super/login" -Method "POST" -Body $loginBody -TestName "SuperAdmin Login (Gateway)"

if ($loginResult.Success) {
    $loginData = $loginResult.Content | ConvertFrom-Json
    $SuperAdminToken = $loginData.jwt
    Write-TestResult "SuperAdmin Token Retrieved" "PASS" "Token length: $($SuperAdminToken.Length)"
}

# Test 2: SuperAdmin Monitoring Dashboard
if ($SuperAdminToken) {
    $headers = @{ "Authorization" = "Bearer $SuperAdminToken" }
    Test-Endpoint -Url "$AuthUrl/auth/super/monitoring/dashboard" -Headers $headers -TestName "SuperAdmin Monitoring Dashboard"
}

# Test 3: Entity Admin Login (will fail until we create one)
Write-TestResult "Entity Admin Login" "SKIP" "No Entity Admin created yet"

# Test 4: Member Login (will fail until we create one)  
Write-TestResult "Member Login" "SKIP" "No Member created yet"

# Test 5: Modern Auth Endpoints
Test-Endpoint -Url "$AuthUrl/auth/api/v2/auth/login" -Method "POST" -Body $loginBody -TestName "Modern Auth Login"

# ========== PHASE 2: ORGANIZATION SERVICE TESTS ==========
Write-Host "`n🏢 PHASE 2: Organization Service Tests" -ForegroundColor $Blue

if ($SuperAdminToken) {
    $headers = @{ "Authorization" = "Bearer $SuperAdminToken" }
    
    # Test 6: Create Organization
    $orgBody = @{
        name = "Test Organization $(Get-Date -Format 'HHmmss')"
        description = "Test Description"
        address = "Test Address"
        contactEmail = "test@example.com"
        contactPhone = "1234567890"
    } | ConvertTo-Json
    
    $orgResult = Test-Endpoint -Url "$GatewayUrl/api/organization/super/organizations" -Method "POST" -Headers $headers -Body $orgBody -TestName "Create Organization"
    
    # Test 7: Get Organizations
    Test-Endpoint -Url "$GatewayUrl/api/organization/super/organizations" -Headers $headers -TestName "Get Organizations"
    
    # Test 8: Create Entity Admin (this was failing before)
    if ($orgResult.Success) {
        $orgData = $orgResult.Content | ConvertFrom-Json
        $orgId = $orgData.organization.id
        
        $entityAdminBody = @{
            organizationId = $orgId
            username = "testadmin$(Get-Date -Format 'HHmmss')"
            password = "admin123"
            email = "testadmin@example.com"
            firstName = "Test"
            lastName = "Admin"
        } | ConvertTo-Json
        
        Test-Endpoint -Url "$GatewayUrl/api/organization/super/entity-admins" -Method "POST" -Headers $headers -Body $entityAdminBody -TestName "Create Entity Admin"
    }
    
    # Test 9: Test Controller Public Endpoint
    Test-Endpoint -Url "$OrgUrl/organization/test/public" -TestName "Organization Test Public Endpoint"
    
    # Test 10: Test Controller Secured Endpoint
    Test-Endpoint -Url "$OrgUrl/organization/test/secured" -Headers $headers -TestName "Organization Test Secured Endpoint"
}

# ========== PHASE 3: USER SERVICE TESTS ==========
Write-Host "`n👥 PHASE 3: User Service Tests" -ForegroundColor $Blue

if ($SuperAdminToken) {
    $headers = @{ 
        "Authorization" = "Bearer $SuperAdminToken"
        "X-User-ID" = "1"  # SuperAdmin user ID
    }
    
    # Test 11: Get All Users
    Test-Endpoint -Url "$UserUrl/user/api/users" -Headers $headers -TestName "Get All Users"
    
    # Test 12: Get User by ID
    Test-Endpoint -Url "$UserUrl/user/api/users/1" -Headers $headers -TestName "Get User by ID"
    
    # Test 13: Create Super Admin
    $superAdminBody = @{
        username = "testsuperadmin$(Get-Date -Format 'HHmmss')"
        password = "admin123"
        email = "testsuperadmin@example.com"
        firstName = "Test"
        lastName = "SuperAdmin"
    } | ConvertTo-Json
    
    Test-Endpoint -Url "$UserUrl/user/api/users/super-admin" -Method "POST" -Headers $headers -Body $superAdminBody -TestName "Create Super Admin"
    
    # Test 14: Create Entity Admin
    $entityAdminBody = @{
        username = "testentityadmin$(Get-Date -Format 'HHmmss')"
        password = "admin123"
        email = "testentityadmin@example.com"
        firstName = "Test"
        lastName = "EntityAdmin"
        organizationId = 1
    } | ConvertTo-Json
    
    Test-Endpoint -Url "$UserUrl/user/api/users/entity-admin" -Method "POST" -Headers $headers -Body $entityAdminBody -TestName "Create Entity Admin (User Service)"
    
    # Test 15: Create Member
    $memberBody = @{
        mobileNumber = "9876543210"
        firstName = "Test"
        lastName = "Member"
        organizationId = 1
    } | ConvertTo-Json
    
    Test-Endpoint -Url "$UserUrl/user/api/users/member" -Method "POST" -Headers $headers -Body $memberBody -TestName "Create Member"
    
    # Test 16: Search Users
    Test-Endpoint -Url "$UserUrl/user/api/users?search=test" -Headers $headers -TestName "Search Users"
    
    # Test 17: Get Users by Type
    Test-Endpoint -Url "$UserUrl/user/api/users?userType=SUPER_ADMIN" -Headers $headers -TestName "Get Users by Type"
    
    # Test 18: Get Users by Organization
    Test-Endpoint -Url "$UserUrl/user/api/users?organizationId=1" -Headers $headers -TestName "Get Users by Organization"
}

# ========== PHASE 4: ATTENDANCE SERVICE TESTS ==========
Write-Host "`n📊 PHASE 4: Attendance Service Tests" -ForegroundColor $Blue

# Test 19: Health Check
Test-Endpoint -Url "$AttendanceUrl/attendance/actuator/health" -TestName "Attendance Service Health"

# Test 20: Test any public endpoints
Test-Endpoint -Url "$AttendanceUrl/attendance/" -TestName "Attendance Service Root"

# ========== PHASE 5: API GATEWAY TESTS ==========
Write-Host "`n🌐 PHASE 5: API Gateway Tests" -ForegroundColor $Blue

# Test 21: Gateway Health
Test-Endpoint -Url "$GatewayUrl/actuator/health" -TestName "API Gateway Health"

# Test 22: Gateway Info
Test-Endpoint -Url "$GatewayUrl/actuator/info" -TestName "API Gateway Info"

# ========== SUMMARY ==========
Write-Host "`n📋 Testing Complete!" -ForegroundColor $Blue
Write-Host "Check the results above for any failing endpoints." -ForegroundColor $Blue
Write-Host "Failed endpoints will need individual investigation and fixes." -ForegroundColor $Blue
