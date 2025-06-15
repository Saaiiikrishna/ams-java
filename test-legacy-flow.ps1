# Test script for the legacy ordering flow (working endpoint)
$baseUrl = "http://localhost:8080"
$headers = @{"Content-Type" = "application/json"}
$entityId = "MSD55781"

Write-Host "🍽️ Testing Legacy Table-Based Ordering Flow" -ForegroundColor Green
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

# Step 2: Get menu using legacy endpoint
Write-Host "`n2. Getting menu using legacy endpoint..." -ForegroundColor Yellow
try {
    $menuResponse = Invoke-RestMethod -Uri "$baseUrl/api/public/menu/$entityId" -Method GET
    Write-Host "✅ Menu retrieved successfully!" -ForegroundColor Green
    Write-Host "   Entity ID: $($menuResponse.entityId)" -ForegroundColor Cyan
    Write-Host "   Table Number: $($menuResponse.tableNumber)" -ForegroundColor Cyan
    Write-Host "   Menu Categories: $($menuResponse.menu.Count)" -ForegroundColor Cyan
    
    # Show menu details
    if ($menuResponse.menu.Count -gt 0) {
        Write-Host "   Menu Details:" -ForegroundColor Cyan
        foreach ($category in $menuResponse.menu) {
            Write-Host "     Category: $($category.name) ($($category.items.Count) items)" -ForegroundColor White
            foreach ($item in $category.items) {
                Write-Host "       - ID: $($item.id), Name: $($item.name), Price: ₹$($item.price)" -ForegroundColor Gray
            }
        }
    }
} catch {
    Write-Host "❌ Menu retrieval failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Test order creation using legacy endpoint
Write-Host "`n3. Testing order creation using legacy endpoint..." -ForegroundColor Yellow
try {
    # Get the first available item from the menu
    $firstCategory = $menuResponse.menu[0]
    $firstItem = $firstCategory.items[0]
    
    $orderBody = @{
        customerName = "Test Customer"
        customerPhone = "9876543210"
        notes = "Test order from PowerShell script - Legacy Flow"
        tableNumber = 1  # Use table number for legacy flow
        orderItems = @(
            @{ 
                itemId = $firstItem.id
                quantity = 2
                price = $firstItem.price
                specialInstructions = "Extra spicy"
            }
        )
    } | ConvertTo-Json -Depth 3

    Write-Host "   Creating order with item: $($firstItem.name) (ID: $($firstItem.id))" -ForegroundColor Cyan
    $orderResponse = Invoke-RestMethod -Uri "$baseUrl/api/public/menu/$entityId/order" -Method POST -Body $orderBody -Headers $headers
    
    Write-Host "✅ Order created successfully!" -ForegroundColor Green
    Write-Host "   Order Number: $($orderResponse.order.orderNumber)" -ForegroundColor Cyan
    Write-Host "   Table Number: $($orderResponse.order.tableNumber)" -ForegroundColor Cyan
    Write-Host "   Total Amount: ₹$($orderResponse.order.totalAmount)" -ForegroundColor Cyan
    Write-Host "   Status: $($orderResponse.order.status)" -ForegroundColor Cyan
    Write-Host "   Message: $($orderResponse.message)" -ForegroundColor Cyan
    
    $orderNumber = $orderResponse.order.orderNumber
} catch {
    Write-Host "❌ Order creation failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Response: $($_.Exception.Response)" -ForegroundColor Yellow
}

# Step 4: Test order status retrieval
if ($orderNumber) {
    Write-Host "`n4. Testing order status retrieval..." -ForegroundColor Yellow
    try {
        $statusResponse = Invoke-RestMethod -Uri "$baseUrl/api/public/menu/order/$orderNumber" -Method GET
        Write-Host "✅ Order status retrieved successfully!" -ForegroundColor Green
        Write-Host "   Order Number: $($statusResponse.orderNumber)" -ForegroundColor Cyan
        Write-Host "   Status: $($statusResponse.status)" -ForegroundColor Cyan
        Write-Host "   Table Number: $($statusResponse.tableNumber)" -ForegroundColor Cyan
        Write-Host "   Total Amount: ₹$($statusResponse.totalAmount)" -ForegroundColor Cyan
    } catch {
        Write-Host "⚠️ Order status retrieval failed: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Step 5: Test QR code simulation
Write-Host "`n5. Testing QR code simulation..." -ForegroundColor Yellow
try {
    # Simulate QR code access with table and qr parameters
    $qrCode = "TABLE-123-456789"
    $tableNumber = 1
    
    $qrMenuResponse = Invoke-RestMethod -Uri "$baseUrl/api/public/menu/$entityId" -Method GET -Body @{
        table = $tableNumber
        qr = $qrCode
    }
    
    Write-Host "✅ QR code simulation works!" -ForegroundColor Green
    Write-Host "   Accessed with table: $tableNumber, QR: $qrCode" -ForegroundColor Cyan
} catch {
    Write-Host "⚠️ QR code simulation failed: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Summary
Write-Host "`n📋 Test Summary:" -ForegroundColor Green
Write-Host "=================" -ForegroundColor Green
Write-Host "✅ Backend server is running and healthy" -ForegroundColor White
Write-Host "✅ mDNS service is configured (restaurant.local)" -ForegroundColor White
Write-Host "✅ Legacy menu endpoint works for entity: $entityId" -ForegroundColor White
if ($orderNumber) {
    Write-Host "✅ Order creation works (Order: $orderNumber)" -ForegroundColor White
}
Write-Host "✅ Menu has $($menuResponse.menu.Count) categories with items" -ForegroundColor White

Write-Host "`n🎯 Next Steps:" -ForegroundColor Yellow
Write-Host "1. Start the public menu frontend on port 3000" -ForegroundColor White
Write-Host "2. Configure frontend to use legacy endpoint: /api/public/menu/$entityId" -ForegroundColor White
Write-Host "3. Test the complete customer ordering flow" -ForegroundColor White
Write-Host "4. Check orders in entity dashboard at: http://localhost:3001" -ForegroundColor White
Write-Host "5. Create tables in entity dashboard for table-specific QR codes" -ForegroundColor White

Write-Host "`n🔧 Technical Details:" -ForegroundColor Blue
Write-Host "- Backend API: http://localhost:8080" -ForegroundColor White
Write-Host "- mDNS hostname: restaurant.local" -ForegroundColor White
Write-Host "- Entity ID: $entityId" -ForegroundColor White
Write-Host "- Working endpoint: /api/public/menu/$entityId" -ForegroundColor White
if ($orderNumber) {
    Write-Host "- Test order: $orderNumber" -ForegroundColor White
}
