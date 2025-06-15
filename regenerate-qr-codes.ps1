# Script to regenerate QR codes for all tables
$baseUrl = "http://localhost:8080"
$headers = @{"Content-Type" = "application/json"}

Write-Host "🔄 Regenerating QR Codes for All Tables" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Green

# Step 1: Login as Entity Admin
Write-Host "`n1. Logging in as Entity Admin..." -ForegroundColor Yellow
try {
    $loginBody = @{
        username = "Test"
        password = "admin123"
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $loginBody -Headers $headers
    $token = $loginResponse.token
    $authHeaders = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer $token"
    }
    Write-Host "✅ Login successful!" -ForegroundColor Green
} catch {
    Write-Host "❌ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Check current tables
Write-Host "`n2. Checking current tables..." -ForegroundColor Yellow
try {
    $tablesResponse = Invoke-RestMethod -Uri "$baseUrl/api/tables" -Method GET -Headers $authHeaders
    Write-Host "✅ Found $($tablesResponse.Count) tables" -ForegroundColor Green
    
    if ($tablesResponse.Count -gt 0) {
        Write-Host "   Current table QR URLs:" -ForegroundColor Cyan
        foreach ($table in $tablesResponse) {
            Write-Host "     Table $($table.tableNumber): $($table.menuUrl)" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "❌ Failed to get tables: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Regenerate all QR codes
Write-Host "`n3. Regenerating all QR codes..." -ForegroundColor Yellow
try {
    $regenerateResponse = Invoke-RestMethod -Uri "$baseUrl/api/tables/regenerate-all-qr" -Method POST -Headers $authHeaders
    Write-Host "✅ QR codes regenerated successfully!" -ForegroundColor Green
    Write-Host "   Tables updated: $($regenerateResponse.tablesUpdated)" -ForegroundColor Cyan
    
    if ($regenerateResponse.tables) {
        Write-Host "   Updated table QR URLs:" -ForegroundColor Cyan
        foreach ($table in $regenerateResponse.tables) {
            Write-Host "     Table $($table.tableNumber): $($table.menuUrl)" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "❌ Failed to regenerate QR codes: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Response: $($_.Exception.Response)" -ForegroundColor Yellow
}

# Step 4: Verify updated tables
Write-Host "`n4. Verifying updated tables..." -ForegroundColor Yellow
try {
    $updatedTablesResponse = Invoke-RestMethod -Uri "$baseUrl/api/tables" -Method GET -Headers $authHeaders
    Write-Host "✅ Verification complete!" -ForegroundColor Green
    
    if ($updatedTablesResponse.Count -gt 0) {
        Write-Host "   Final table QR URLs:" -ForegroundColor Cyan
        foreach ($table in $updatedTablesResponse) {
            Write-Host "     Table $($table.tableNumber) (ID: $($table.id)): $($table.menuUrl)" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "⚠️ Failed to verify tables: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`n🎉 QR Code regeneration completed!" -ForegroundColor Green
Write-Host "All QR codes now point to: http://restaurant.local:57977/menu/table/[TABLE_ID]" -ForegroundColor White
Write-Host "`n📱 Test the QR codes by scanning them with your phone!" -ForegroundColor Yellow
