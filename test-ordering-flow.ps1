# Test script for the complete table-based ordering flow
$baseUrl = "http://localhost:8080"
$headers = @{"Content-Type" = "application/json"}

Write-Host "🚀 Testing Complete Table-Based Ordering Flow" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

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
    Write-Host "✅ Login successful! Token: $($token.Substring(0,20))..." -ForegroundColor Green
} catch {
    Write-Host "❌ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Check table management permissions
Write-Host "`n2. Checking table management permissions..." -ForegroundColor Yellow
try {
    $permissionResponse = Invoke-RestMethod -Uri "$baseUrl/api/entity/permissions/check/TABLE_MANAGEMENT" -Method GET -Headers $authHeaders
    if ($permissionResponse.hasPermission) {
        Write-Host "✅ Table management permission granted" -ForegroundColor Green
    } else {
        Write-Host "❌ No table management permission" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Permission check failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Create tables
Write-Host "`n3. Creating restaurant tables..." -ForegroundColor Yellow
$tableIds = @()
for ($i = 1; $i -le 3; $i++) {
    try {
        $tableBody = @{
            tableNumber = $i
            capacity = 4
            locationDescription = "Near window $i"
            isActive = $true
        } | ConvertTo-Json

        $tableResponse = Invoke-RestMethod -Uri "$baseUrl/api/tables" -Method POST -Body $tableBody -Headers $authHeaders
        $tableIds += $tableResponse.id
        Write-Host "✅ Table $i created with ID: $($tableResponse.id)" -ForegroundColor Green
        Write-Host "   QR Code URL: $($tableResponse.menuUrl)" -ForegroundColor Cyan
    } catch {
        Write-Host "⚠️ Table $i creation failed (might already exist): $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Step 4: Get existing tables if creation failed
if ($tableIds.Count -eq 0) {
    Write-Host "`n4. Getting existing tables..." -ForegroundColor Yellow
    try {
        $existingTables = Invoke-RestMethod -Uri "$baseUrl/api/tables" -Method GET -Headers $authHeaders
        $tableIds = $existingTables | ForEach-Object { $_.id }
        Write-Host "✅ Found $($tableIds.Count) existing tables" -ForegroundColor Green
    } catch {
        Write-Host "❌ Failed to get existing tables: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# Step 5: Check menu management permissions
Write-Host "`n5. Checking menu management permissions..." -ForegroundColor Yellow
try {
    $menuPermissionResponse = Invoke-RestMethod -Uri "$baseUrl/api/entity/permissions/check/MENU_MANAGEMENT" -Method GET -Headers $authHeaders
    if ($menuPermissionResponse.hasPermission) {
        Write-Host "✅ Menu management permission granted" -ForegroundColor Green
    } else {
        Write-Host "❌ No menu management permission" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Menu permission check failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 6: Create menu categories and items
Write-Host "`n6. Creating menu categories and items..." -ForegroundColor Yellow
$categoryId = $null
try {
    # Create category
    $categoryBody = @{
        name = "Test Appetizers"
        description = "Delicious starters"
        displayOrder = 1
        isActive = $true
    } | ConvertTo-Json

    $categoryResponse = Invoke-RestMethod -Uri "$baseUrl/api/categories" -Method POST -Body $categoryBody -Headers $authHeaders
    $categoryId = $categoryResponse.id
    Write-Host "✅ Category created with ID: $categoryId" -ForegroundColor Green

    # Create menu items
    $items = @(
        @{ name = "Spring Rolls"; price = 150; description = "Crispy vegetable spring rolls" },
        @{ name = "Chicken Wings"; price = 250; description = "Spicy buffalo wings" },
        @{ name = "Garlic Bread"; price = 120; description = "Fresh baked garlic bread" }
    )

    foreach ($item in $items) {
        $itemBody = @{
            name = $item.name
            description = $item.description
            price = $item.price
            categoryId = $categoryId
            displayOrder = 1
            isActive = $true
            isAvailable = $true
        } | ConvertTo-Json

        $itemResponse = Invoke-RestMethod -Uri "$baseUrl/api/items" -Method POST -Body $itemBody -Headers $authHeaders
        Write-Host "✅ Item '$($item.name)' created with ID: $($itemResponse.id)" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️ Menu creation failed (might already exist): $($_.Exception.Message)" -ForegroundColor Yellow
}

# Step 7: Test table menu endpoint
Write-Host "`n7. Testing table menu endpoint..." -ForegroundColor Yellow
if ($tableIds.Count -gt 0) {
    $testTableId = $tableIds[0]
    try {
        $menuResponse = Invoke-RestMethod -Uri "$baseUrl/api/public/tables/$testTableId/menu" -Method GET
        Write-Host "✅ Table menu retrieved successfully" -ForegroundColor Green
        Write-Host "   Table ID: $($menuResponse.tableId)" -ForegroundColor Cyan
        Write-Host "   Table Number: $($menuResponse.tableNumber)" -ForegroundColor Cyan
        Write-Host "   Menu Categories: $($menuResponse.menu.Count)" -ForegroundColor Cyan
    } catch {
        Write-Host "❌ Table menu test failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n🎉 Test completed! Check the results above." -ForegroundColor Green
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Start the public menu frontend on port 3000" -ForegroundColor White
Write-Host "2. Access table menu via: http://restaurant.local:3000/menu/table/$testTableId" -ForegroundColor White
Write-Host "3. Test the complete ordering flow" -ForegroundColor White
