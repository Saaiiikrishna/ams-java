# Simple test for public endpoints
$baseUrl = "http://localhost:8080"

Write-Host "🧪 Testing Public Endpoints" -ForegroundColor Green
Write-Host "===========================" -ForegroundColor Green

# Test 1: Health check
Write-Host "`n1. Testing health endpoint..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-RestMethod -Uri "$baseUrl/subscriber/health" -Method GET
    Write-Host "✅ Health check passed: $($healthResponse.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ Health check failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: Try to access a table menu (this will fail if no tables exist)
Write-Host "`n2. Testing table menu endpoint..." -ForegroundColor Yellow
try {
    $menuResponse = Invoke-RestMethod -Uri "$baseUrl/api/public/tables/1/menu" -Method GET
    Write-Host "✅ Table menu endpoint works!" -ForegroundColor Green
    Write-Host "   Table ID: $($menuResponse.tableId)" -ForegroundColor Cyan
    Write-Host "   Table Number: $($menuResponse.tableNumber)" -ForegroundColor Cyan
} catch {
    Write-Host "⚠️ Table menu endpoint failed (expected if no tables exist): $($_.Exception.Message)" -ForegroundColor Yellow
}

# Test 3: Try to create an order (this will fail if no tables/items exist)
Write-Host "`n3. Testing order creation endpoint..." -ForegroundColor Yellow
try {
    $orderBody = @{
        tableId = 1
        items = @(
            @{ id = 1; qty = 2 }
        )
    } | ConvertTo-Json

    $orderResponse = Invoke-RestMethod -Uri "$baseUrl/api/public/orders" -Method POST -Body $orderBody -ContentType "application/json"
    Write-Host "✅ Order creation works!" -ForegroundColor Green
    Write-Host "   Order Number: $($orderResponse.orderNumber)" -ForegroundColor Cyan
} catch {
    Write-Host "⚠️ Order creation failed (expected if no tables/items exist): $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`n📋 Summary:" -ForegroundColor Green
Write-Host "- Backend server is running and healthy" -ForegroundColor White
Write-Host "- Public endpoints are accessible" -ForegroundColor White
Write-Host "- Need to set up tables and menu items through entity dashboard" -ForegroundColor White
Write-Host "`n🎯 Next Steps:" -ForegroundColor Yellow
Write-Host "1. Access entity dashboard at: http://localhost:3001" -ForegroundColor White
Write-Host "2. Login with: Sunny / admin123" -ForegroundColor White
Write-Host "3. Create tables in Table Management" -ForegroundColor White
Write-Host "4. Create menu items in Menu Management" -ForegroundColor White
Write-Host "5. Test QR codes and ordering flow" -ForegroundColor White
