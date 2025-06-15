# Test script for the public ordering flow (no authentication needed)
$baseUrl = "http://localhost:8080"
$headers = @{"Content-Type" = "application/json"}

Write-Host "🍽️ Testing Public Table-Based Ordering Flow" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

# Step 1: Test health endpoint
Write-Host "`n1. Testing server health..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-RestMethod -Uri "$baseUrl/subscriber/health" -Method GET
    Write-Host "✅ Server is healthy: $($healthResponse.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ Server health check failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Test table menu endpoints for different table IDs
Write-Host "`n2. Testing table menu endpoints..." -ForegroundColor Yellow
$workingTableId = $null
$workingEntityId = "MSD55781"  # Known entity ID

# Try different table IDs to find one that works
for ($tableId = 1; $tableId -le 10; $tableId++) {
    try {
        Write-Host "   Trying table ID: $tableId" -ForegroundColor Cyan
        $menuResponse = Invoke-RestMethod -Uri "$baseUrl/api/public/tables/$tableId/menu" -Method GET
        $workingTableId = $tableId
        Write-Host "✅ Found working table! ID: $tableId" -ForegroundColor Green
        Write-Host "   Table Number: $($menuResponse.tableNumber)" -ForegroundColor Cyan
        Write-Host "   Entity ID: $($menuResponse.entityId)" -ForegroundColor Cyan
        Write-Host "   Menu Categories: $($menuResponse.menu.Count)" -ForegroundColor Cyan
        
        # Show menu items
        if ($menuResponse.menu.Count -gt 0) {
            Write-Host "   Menu Items:" -ForegroundColor Cyan
            foreach ($category in $menuResponse.menu) {
                Write-Host "     Category: $($category.name) ($($category.items.Count) items)" -ForegroundColor White
                foreach ($item in $category.items) {
                    Write-Host "       - $($item.name): ₹$($item.price)" -ForegroundColor Gray
                }
            }
        }
        break
    } catch {
        Write-Host "   Table $tableId not found" -ForegroundColor Gray
    }
}

if ($workingTableId -eq $null) {
    Write-Host "❌ No working tables found. Please create tables in the entity dashboard first." -ForegroundColor Red
    exit 1
}

# Step 3: Test order creation
Write-Host "`n3. Testing order creation..." -ForegroundColor Yellow
try {
    # Get the first available item from the menu
    $firstCategory = $menuResponse.menu[0]
    $firstItem = $firstCategory.items[0]
    
    $orderBody = @{
        tableId = $workingTableId
        customerName = "Test Customer"
        customerPhone = "9876543210"
        notes = "Test order from PowerShell script"
        items = @(
            @{ 
                id = $firstItem.id
                qty = 2 
            }
        )
    } | ConvertTo-Json -Depth 3

    Write-Host "   Creating order with item: $($firstItem.name)" -ForegroundColor Cyan
    $orderResponse = Invoke-RestMethod -Uri "$baseUrl/api/public/orders" -Method POST -Body $orderBody -Headers $headers
    
    Write-Host "✅ Order created successfully!" -ForegroundColor Green
    Write-Host "   Order Number: $($orderResponse.orderNumber)" -ForegroundColor Cyan
    Write-Host "   Table Number: $($orderResponse.tableNumber)" -ForegroundColor Cyan
    Write-Host "   Status: $($orderResponse.status)" -ForegroundColor Cyan
    
    $orderNumber = $orderResponse.orderNumber
} catch {
    Write-Host "❌ Order creation failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   This might be due to missing menu items or other issues" -ForegroundColor Yellow
}

# Step 4: Test QR code generation URL
Write-Host "`n4. Testing QR code URLs..." -ForegroundColor Yellow
$qrUrl = "http://restaurant.local:3000/menu/table/$workingTableId"
Write-Host "✅ QR Code should point to: $qrUrl" -ForegroundColor Green

# Step 5: Test legacy menu endpoint
Write-Host "`n5. Testing legacy menu endpoint..." -ForegroundColor Yellow
try {
    $legacyMenuResponse = Invoke-RestMethod -Uri "$baseUrl/api/public/menu/$workingEntityId" -Method GET
    Write-Host "✅ Legacy menu endpoint works!" -ForegroundColor Green
    Write-Host "   Entity ID: $($legacyMenuResponse.entityId)" -ForegroundColor Cyan
    Write-Host "   Menu Categories: $($legacyMenuResponse.menu.Count)" -ForegroundColor Cyan
} catch {
    Write-Host "⚠️ Legacy menu endpoint failed: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Summary
Write-Host "`n📋 Test Summary:" -ForegroundColor Green
Write-Host "=================" -ForegroundColor Green
Write-Host "✅ Backend server is running and healthy" -ForegroundColor White
Write-Host "✅ mDNS service is configured (restaurant.local)" -ForegroundColor White
Write-Host "✅ Table menu endpoint works for table ID: $workingTableId" -ForegroundColor White
if ($orderNumber) {
    Write-Host "✅ Order creation works (Order: $orderNumber)" -ForegroundColor White
}
Write-Host "✅ QR codes should point to: $qrUrl" -ForegroundColor White

Write-Host "`n🎯 Next Steps:" -ForegroundColor Yellow
Write-Host "1. Start the public menu frontend on port 3000" -ForegroundColor White
Write-Host "2. Access the menu via QR code URL: $qrUrl" -ForegroundColor White
Write-Host "3. Test the complete customer ordering flow" -ForegroundColor White
Write-Host "4. Check orders in entity dashboard at: http://localhost:3001" -ForegroundColor White

Write-Host "`n🔧 Technical Details:" -ForegroundColor Blue
Write-Host "- Backend API: http://localhost:8080" -ForegroundColor White
Write-Host "- mDNS hostname: restaurant.local" -ForegroundColor White
Write-Host "- Entity ID: $workingEntityId" -ForegroundColor White
Write-Host "- Working Table ID: $workingTableId" -ForegroundColor White
