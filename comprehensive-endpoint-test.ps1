# 🎯 COMPREHENSIVE ENDPOINT TESTING SCRIPT
# Tests complete workflow: SuperAdmin → EntityAdmin → Member
Write-Host "🎯 COMPREHENSIVE MICROSERVICES ENDPOINT TESTING" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Gray

# Global variables
$baseUrl = "http://localhost:8080"
$directAuthUrl = "http://localhost:8081"
$directOrgUrl = "http://localhost:8082"
$directUserUrl = "http://localhost:8083"
$directAttendanceUrl = "http://localhost:8084"

$testResults = @()
$issuesFound = @()

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [string]$ContentType = "application/json"
    )
    
    try {
        if ($Body) {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $Headers -Body $Body -ContentType $ContentType
        } else {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $Headers
        }
        
        Write-Host "✅ $Name - SUCCESS" -ForegroundColor Green
        $script:testResults += @{Name = $Name; Status = "SUCCESS"; Response = $response}
        return $response
    } catch {
        Write-Host "❌ $Name - FAILED: $($_.Exception.Message)" -ForegroundColor Red
        $script:testResults += @{Name = $Name; Status = "FAILED"; Error = $_.Exception.Message}
        $script:issuesFound += $Name
        return $null
    }
}

# =============================================================================
# TASK 3.1: SUPERADMIN OPERATIONS
# =============================================================================
Write-Host "`n🔐 TASK 3.1: SUPERADMIN OPERATIONS" -ForegroundColor Yellow
Write-Host "-----------------------------------" -ForegroundColor Gray

# 3.1.1: Login as SuperAdmin
Write-Host "`n3.1.1 Testing SuperAdmin Login..." -ForegroundColor White
$loginData = @{
    username = "superadmin"
    password = "admin123"
} | ConvertTo-Json

# Test via API Gateway
$superAdminAuth = Test-Endpoint -Name "SuperAdmin Login via API Gateway" -Url "$baseUrl/api/auth/super/login" -Method POST -Body $loginData
if ($superAdminAuth) {
    $superAdminToken = $superAdminAuth.jwt
    $superAdminHeaders = @{"Authorization" = "Bearer $superAdminToken"; "Content-Type" = "application/json"}
    Write-Host "   SuperAdmin Token: $($superAdminToken.Substring(0,50))..." -ForegroundColor Cyan
}

# Test direct access for comparison
$superAdminAuthDirect = Test-Endpoint -Name "SuperAdmin Login Direct" -Url "$directAuthUrl/auth/super/auth/login" -Method POST -Body $loginData

# 3.1.2: Create new entity "Anu house"
Write-Host "`n3.1.2 Creating new entity 'Anu house'..." -ForegroundColor White
if ($superAdminToken) {
    $entityData = @{
        name = "Anu house"
        description = "Test entity for comprehensive testing"
        address = "123 Test Street"
        contactEmail = "anu@example.com"
        contactPhone = "1234567890"
    } | ConvertTo-Json
    
    # Test via API Gateway
    $newEntity = Test-Endpoint -Name "Create Entity via API Gateway" -Url "$baseUrl/api/organization/super/organizations" -Method POST -Headers $superAdminHeaders -Body $entityData

    # Test direct access
    $newEntityDirect = Test-Endpoint -Name "Create Entity Direct" -Url "$directOrgUrl/organization/super/organizations" -Method POST -Headers $superAdminHeaders -Body $entityData
    
    if ($newEntity) {
        $entityId = $newEntity.id
        Write-Host "   Created Entity ID: $entityId" -ForegroundColor Cyan
    }
}

# 3.1.3: Create EntityAdmin "Anu" with password "admin123"
Write-Host "`n3.1.3 Creating EntityAdmin 'Anu'..." -ForegroundColor White
if ($superAdminToken -and $entityId) {
    $entityAdminData = @{
        username = "Anu"
        password = "admin123"
        firstName = "Anu"
        lastName = "Admin"
        email = "anu.admin@example.com"
        organizationId = $entityId
    } | ConvertTo-Json
    
    # Test via API Gateway
    $newEntityAdmin = Test-Endpoint -Name "Create EntityAdmin via API Gateway" -Url "$baseUrl/api/organization/super/entity-admins" -Method POST -Headers $superAdminHeaders -Body $entityAdminData

    # Test direct access
    $newEntityAdminDirect = Test-Endpoint -Name "Create EntityAdmin Direct" -Url "$directOrgUrl/organization/super/entity-admins" -Method POST -Headers $superAdminHeaders -Body $entityAdminData
    
    if ($newEntityAdmin) {
        Write-Host "   Created EntityAdmin: $($newEntityAdmin.username)" -ForegroundColor Cyan
    }
}

# =============================================================================
# TASK 3.2: ENTITYADMIN OPERATIONS
# =============================================================================
Write-Host "`n🏢 TASK 3.2: ENTITYADMIN OPERATIONS" -ForegroundColor Yellow
Write-Host "------------------------------------" -ForegroundColor Gray

# 3.2.1: Login as EntityAdmin (Anu/admin123)
Write-Host "`n3.2.1 Testing EntityAdmin Login..." -ForegroundColor White
$entityAdminLoginData = @{
    username = "Anu"
    password = "admin123"
} | ConvertTo-Json

# Test via API Gateway
$entityAdminAuth = Test-Endpoint -Name "EntityAdmin Login via API Gateway" -Url "$baseUrl/api/auth/login" -Method POST -Body $entityAdminLoginData

# Test direct access
$entityAdminAuthDirect = Test-Endpoint -Name "EntityAdmin Login Direct" -Url "$directAuthUrl/auth/login" -Method POST -Body $entityAdminLoginData

if ($entityAdminAuth) {
    $entityAdminToken = $entityAdminAuth.jwt
    $entityAdminHeaders = @{"Authorization" = "Bearer $entityAdminToken"; "Content-Type" = "application/json"}
    Write-Host "   EntityAdmin Token: $($entityAdminToken.Substring(0,50))..." -ForegroundColor Cyan
}

# 3.2.2: Register new NFC card with dummy UID
Write-Host "`n3.2.2 Registering NFC card..." -ForegroundColor White
if ($entityAdminToken) {
    $nfcCardData = @{
        uid = "DUMMY123456789"
        description = "Test NFC Card for Anu house"
        isActive = $true
    } | ConvertTo-Json
    
    # Note: Need to find correct NFC registration endpoint
    # This might be in User Service or Organization Service
    Write-Host "   ⚠️ NFC Card registration endpoint needs to be identified" -ForegroundColor Yellow
}

Write-Host "`n📊 TESTING SUMMARY SO FAR:" -ForegroundColor Cyan
Write-Host "Total Tests: $($testResults.Count)" -ForegroundColor Gray
Write-Host "Successful: $(($testResults | Where-Object {$_.Status -eq 'SUCCESS'}).Count)" -ForegroundColor Green
Write-Host "Failed: $(($testResults | Where-Object {$_.Status -eq 'FAILED'}).Count)" -ForegroundColor Red

if ($issuesFound.Count -gt 0) {
    Write-Host "`n🚨 ISSUES FOUND:" -ForegroundColor Red
    $issuesFound | ForEach-Object { Write-Host "   - $_" -ForegroundColor Yellow }
}

Write-Host "`n⏸️ PAUSING FOR ANALYSIS..." -ForegroundColor Cyan
Write-Host "This script will be extended to complete all testing tasks." -ForegroundColor Gray
