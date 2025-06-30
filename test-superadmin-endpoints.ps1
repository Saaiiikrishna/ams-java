#!/usr/bin/env pwsh

# Test script for SuperAdmin endpoints in Organization Service
# Tests entity creation and entity admin assignment functionality

Write-Host "=== SuperAdmin Endpoints Test Script ===" -ForegroundColor Yellow
Write-Host "Testing Organization Service SuperAdmin functionality" -ForegroundColor Yellow
Write-Host ""

# First, get a SuperAdmin token from auth service
Write-Host "Step 1: Getting SuperAdmin token..." -ForegroundColor Cyan
$loginBody = @{
    username = "superadmin"
    password = "admin123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest -Uri 'http://localhost:8081/auth/super/auth/login' -Method POST -Body $loginBody -ContentType 'application/json' -UseBasicParsing
    $loginData = $loginResponse.Content | ConvertFrom-Json
    $token = $loginData.jwt
    Write-Host "✅ SuperAdmin login successful" -ForegroundColor Green
    Write-Host "Token: $($token.Substring(0, 50))..." -ForegroundColor Gray
} catch {
    Write-Host "❌ SuperAdmin login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Set up headers with JWT token
$headers = @{ "Authorization" = "Bearer $token" }

Write-Host "`n=== Testing Entity Creation Endpoints ===" -ForegroundColor Yellow

# Test 1: Create Organization via /super/organizations
Write-Host "`nTest 1: POST /super/organizations..." -ForegroundColor Cyan
$createOrgBody = @{
    name = "Test Organization Alpha"
    address = "123 Test Street, Test City"
    contactEmail = "admin@testalpha.com"
    contactPhone = "1234567890"
    latitude = 40.7128
    longitude = -74.0060
} | ConvertTo-Json

try {
    $createResponse = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/organizations' -Method POST -Headers $headers -Body $createOrgBody -ContentType 'application/json' -UseBasicParsing
    $createData = $createResponse.Content | ConvertFrom-Json
    Write-Host "✅ POST /super/organizations: SUCCESS ($($createResponse.StatusCode))" -ForegroundColor Green
    Write-Host "Organization ID: $($createData.organization.id)" -ForegroundColor Gray
    Write-Host "Entity ID: $($createData.organization.entityId)" -ForegroundColor Gray
    $organizationId = $createData.organization.id
    $entityId = $createData.organization.entityId
} catch {
    Write-Host "❌ POST /super/organizations: FAILED - $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $errorResponse = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorResponse)
        $errorContent = $reader.ReadToEnd()
        Write-Host "Error details: $errorContent" -ForegroundColor Red
    }
}

# Test 2: Create Organization via /super/entities (alias)
Write-Host "`nTest 2: POST /super/entities (alias)..." -ForegroundColor Cyan
$createOrgBody2 = @{
    name = "Test Organization Beta"
    address = "456 Beta Avenue, Beta City"
    contactEmail = "admin@testbeta.com"
    contactPhone = "0987654321"
    latitude = 34.0522
    longitude = -118.2437
} | ConvertTo-Json

try {
    $createResponse2 = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/entities' -Method POST -Headers $headers -Body $createOrgBody2 -ContentType 'application/json' -UseBasicParsing
    $createData2 = $createResponse2.Content | ConvertFrom-Json
    Write-Host "✅ POST /super/entities: SUCCESS ($($createResponse2.StatusCode))" -ForegroundColor Green
    Write-Host "Organization ID: $($createData2.organization.id)" -ForegroundColor Gray
    Write-Host "Entity ID: $($createData2.organization.entityId)" -ForegroundColor Gray
    $organizationId2 = $createData2.organization.id
} catch {
    Write-Host "❌ POST /super/entities: FAILED - $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $errorResponse = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorResponse)
        $errorContent = $reader.ReadToEnd()
        Write-Host "Error details: $errorContent" -ForegroundColor Red
    }
}

# Test 3: Get all organizations
Write-Host "`nTest 3: GET /super/organizations..." -ForegroundColor Cyan
try {
    $getResponse = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/organizations' -Headers $headers -UseBasicParsing
    $getData = $getResponse.Content | ConvertFrom-Json
    Write-Host "✅ GET /super/organizations: SUCCESS ($($getResponse.StatusCode))" -ForegroundColor Green
    Write-Host "Total organizations: $($getData.totalCount)" -ForegroundColor Gray
} catch {
    Write-Host "❌ GET /super/organizations: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Get all entities (alias)
Write-Host "`nTest 4: GET /super/entities (alias)..." -ForegroundColor Cyan
try {
    $getResponse2 = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/entities' -Headers $headers -UseBasicParsing
    $getData2 = $getResponse2.Content | ConvertFrom-Json
    Write-Host "✅ GET /super/entities: SUCCESS ($($getResponse2.StatusCode))" -ForegroundColor Green
    Write-Host "Total entities: $($getData2.totalCount)" -ForegroundColor Gray
} catch {
    Write-Host "❌ GET /super/entities: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== Testing Entity Admin Assignment Endpoints ===" -ForegroundColor Yellow

# Test 5: Create Entity Admin for first organization
if ($organizationId) {
    Write-Host "`nTest 5: POST /super/entity-admins (for Organization $organizationId)..." -ForegroundColor Cyan
    $createAdminBody = @{
        username = "admin_alpha"
        password = "admin123"
        email = "admin@testalpha.com"
        firstName = "Alpha"
        lastName = "Admin"
        mobileNumber = "1234567890"
        organizationId = $organizationId
    } | ConvertTo-Json

    try {
        $adminResponse = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/entity-admins' -Method POST -Headers $headers -Body $createAdminBody -ContentType 'application/json' -UseBasicParsing
        $adminData = $adminResponse.Content | ConvertFrom-Json
        Write-Host "✅ POST /super/entity-admins: SUCCESS ($($adminResponse.StatusCode))" -ForegroundColor Green
        Write-Host "Entity Admin ID: $($adminData.entityAdmin.id)" -ForegroundColor Gray
        Write-Host "Username: $($adminData.entityAdmin.username)" -ForegroundColor Gray
        Write-Host "Organization: $($adminData.entityAdmin.organizationName)" -ForegroundColor Gray
    } catch {
        Write-Host "❌ POST /super/entity-admins: FAILED - $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $errorResponse = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorResponse)
            $errorContent = $reader.ReadToEnd()
            Write-Host "Error details: $errorContent" -ForegroundColor Red
        }
    }
}

# Test 6: Create Entity Admin for second organization
if ($organizationId2) {
    Write-Host "`nTest 6: POST /super/entity-admins (for Organization $organizationId2)..." -ForegroundColor Cyan
    $createAdminBody2 = @{
        username = "admin_beta"
        password = "admin123"
        email = "admin@testbeta.com"
        firstName = "Beta"
        lastName = "Admin"
        mobileNumber = "0987654321"
        organizationId = $organizationId2
    } | ConvertTo-Json

    try {
        $adminResponse2 = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/entity-admins' -Method POST -Headers $headers -Body $createAdminBody2 -ContentType 'application/json' -UseBasicParsing
        $adminData2 = $adminResponse2.Content | ConvertFrom-Json
        Write-Host "✅ POST /super/entity-admins: SUCCESS ($($adminResponse2.StatusCode))" -ForegroundColor Green
        Write-Host "Entity Admin ID: $($adminData2.entityAdmin.id)" -ForegroundColor Gray
        Write-Host "Username: $($adminData2.entityAdmin.username)" -ForegroundColor Gray
        Write-Host "Organization: $($adminData2.entityAdmin.organizationName)" -ForegroundColor Gray
    } catch {
        Write-Host "❌ POST /super/entity-admins: FAILED - $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $errorResponse = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorResponse)
            $errorContent = $reader.ReadToEnd()
            Write-Host "Error details: $errorContent" -ForegroundColor Red
        }
    }
}

Write-Host "`n=== Testing Security Validation ===" -ForegroundColor Yellow

# Test 7: Try to create organization without token (should fail)
Write-Host "`nTest 7: POST /super/organizations without token (should fail)..." -ForegroundColor Cyan
try {
    $unauthorizedResponse = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/organizations' -Method POST -Body $createOrgBody -ContentType 'application/json' -UseBasicParsing
    Write-Host "❌ Security test failed - request should have been rejected" -ForegroundColor Red
} catch {
    Write-Host "✅ Security test passed - unauthorized request rejected" -ForegroundColor Green
}

# Test 8: Try to create entity admin without token (should fail)
Write-Host "`nTest 8: POST /super/entity-admins without token (should fail)..." -ForegroundColor Cyan
try {
    $unauthorizedResponse2 = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/entity-admins' -Method POST -Body $createAdminBody -ContentType 'application/json' -UseBasicParsing
    Write-Host "❌ Security test failed - request should have been rejected" -ForegroundColor Red
} catch {
    Write-Host "✅ Security test passed - unauthorized request rejected" -ForegroundColor Green
}

Write-Host "`n=== Test Summary ===" -ForegroundColor Yellow
Write-Host "✅ Entity creation endpoints tested" -ForegroundColor Green
Write-Host "✅ Entity admin assignment endpoints tested" -ForegroundColor Green
Write-Host "✅ Security validation tested" -ForegroundColor Green
Write-Host "✅ Both /organizations and /entities aliases tested" -ForegroundColor Green
Write-Host ""
Write-Host "All SuperAdmin functionality tests completed!" -ForegroundColor Yellow
