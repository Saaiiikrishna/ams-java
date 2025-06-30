# Endpoint Comparison Script
# Compares monolithic backend endpoints with microservices endpoints

param(
    [string]$MonolithicUrl = "http://localhost:8080",
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$OutputFile = "endpoint-comparison-report.json"
)

# Color functions
function Write-Success { param($Message) Write-Host $Message -ForegroundColor Green }
function Write-Error { param($Message) Write-Host $Message -ForegroundColor Red }
function Write-Info { param($Message) Write-Host $Message -ForegroundColor Cyan }
function Write-Warning { param($Message) Write-Host $Message -ForegroundColor Yellow }

Write-Info "🔍 Starting Endpoint Comparison Analysis"
Write-Info "Monolithic URL: $MonolithicUrl"
Write-Info "Gateway URL: $GatewayUrl"

# Define expected endpoints based on code analysis
$MonolithicEndpoints = @{
    # Authentication & Authorization
    "POST /api/auth/login" = @{ Service = "Auth"; Description = "Entity admin login"; Critical = $true }
    "POST /api/auth/refresh" = @{ Service = "Auth"; Description = "Refresh token"; Critical = $true }
    "POST /super/auth/login" = @{ Service = "Auth"; Description = "Super admin login"; Critical = $true }
    "POST /super/auth/refresh" = @{ Service = "Auth"; Description = "Super admin refresh"; Critical = $true }
    
    # Organization Management
    "GET /super/entities" = @{ Service = "Organization"; Description = "Get all organizations"; Critical = $true }
    "POST /super/entities" = @{ Service = "Organization"; Description = "Create organization"; Critical = $true }
    "PUT /super/entities/{id}" = @{ Service = "Organization"; Description = "Update organization"; Critical = $true }
    "DELETE /super/entities/{id}" = @{ Service = "Organization"; Description = "Delete organization"; Critical = $false }
    "GET /super/permissions" = @{ Service = "Organization"; Description = "Get permissions"; Critical = $true }
    
    # Subscriber Management
    "POST /subscriber/login" = @{ Service = "Subscriber"; Description = "Subscriber login"; Critical = $true }
    "GET /subscriber/dashboard" = @{ Service = "Subscriber"; Description = "Subscriber dashboard"; Critical = $true }
    "GET /api/subscribers" = @{ Service = "Subscriber"; Description = "Get subscribers"; Critical = $true }
    "POST /api/subscribers" = @{ Service = "Subscriber"; Description = "Create subscriber"; Critical = $true }
    "PUT /api/subscribers/{id}" = @{ Service = "Subscriber"; Description = "Update subscriber"; Critical = $true }
    
    # Attendance Management
    "GET /api/sessions" = @{ Service = "Attendance"; Description = "Get attendance sessions"; Critical = $true }
    "POST /api/sessions" = @{ Service = "Attendance"; Description = "Create attendance session"; Critical = $true }
    "POST /api/checkin/qr" = @{ Service = "Attendance"; Description = "QR code check-in"; Critical = $true }
    "POST /api/checkin/face" = @{ Service = "Attendance"; Description = "Face recognition check-in"; Critical = $true }
    "POST /api/checkin/wifi" = @{ Service = "Attendance"; Description = "WiFi check-in"; Critical = $true }
    "GET /api/reports" = @{ Service = "Attendance"; Description = "Attendance reports"; Critical = $true }
    "GET /mobile/sessions" = @{ Service = "Attendance"; Description = "Mobile sessions"; Critical = $true }
    "POST /mobile/checkin/qr" = @{ Service = "Attendance"; Description = "Mobile QR check-in"; Critical = $true }
    
    # Menu Management
    "GET /api/menu/categories" = @{ Service = "Menu"; Description = "Get menu categories"; Critical = $true }
    "POST /api/menu/categories" = @{ Service = "Menu"; Description = "Create menu category"; Critical = $true }
    "GET /api/menu/items" = @{ Service = "Menu"; Description = "Get menu items"; Critical = $true }
    "POST /api/menu/items" = @{ Service = "Menu"; Description = "Create menu item"; Critical = $true }
    "GET /api/public/menu" = @{ Service = "Menu"; Description = "Public menu access"; Critical = $true }
    "GET /api/public/menu/categories" = @{ Service = "Menu"; Description = "Public menu categories"; Critical = $true }
    "GET /api/public/menu/items" = @{ Service = "Menu"; Description = "Public menu items"; Critical = $true }
    
    # Order Management
    "POST /api/orders" = @{ Service = "Order"; Description = "Create order"; Critical = $true }
    "GET /api/orders" = @{ Service = "Order"; Description = "Get orders"; Critical = $true }
    "PUT /api/orders/{id}" = @{ Service = "Order"; Description = "Update order"; Critical = $true }
    "GET /api/orders/{id}/status" = @{ Service = "Order"; Description = "Get order status"; Critical = $true }
    
    # Table Management
    "GET /api/tables" = @{ Service = "Table"; Description = "Get tables"; Critical = $true }
    "POST /api/tables" = @{ Service = "Table"; Description = "Create table"; Critical = $true }
    "GET /api/tables/{id}/qr" = @{ Service = "Table"; Description = "Get table QR code"; Critical = $true }
    "PUT /api/tables/{id}" = @{ Service = "Table"; Description = "Update table"; Critical = $true }
    
    # NFC Management
    "POST /api/nfc/register" = @{ Service = "Subscriber"; Description = "Register NFC card"; Critical = $true }
    "GET /api/nfc/cards" = @{ Service = "Subscriber"; Description = "Get NFC cards"; Critical = $true }
    "POST /api/nfc/checkin" = @{ Service = "Attendance"; Description = "NFC check-in"; Critical = $true }
    
    # Face Recognition
    "POST /api/face/register" = @{ Service = "Attendance"; Description = "Register face"; Critical = $true }
    "POST /api/face/recognize" = @{ Service = "Attendance"; Description = "Recognize face"; Critical = $true }
    "GET /api/face/settings" = @{ Service = "Attendance"; Description = "Face recognition settings"; Critical = $false }
    
    # File Management
    "GET /api/files/profile/{filename}" = @{ Service = "File"; Description = "Serve profile photos"; Critical = $true }
    "GET /api/files/face/{filename}" = @{ Service = "File"; Description = "Serve face images"; Critical = $true }
    
    # System Management
    "GET /api/grpc/status" = @{ Service = "System"; Description = "gRPC service status"; Critical = $false }
    "POST /api/grpc/test-connectivity" = @{ Service = "System"; Description = "Test gRPC connectivity"; Critical = $false }
    "GET /discovery" = @{ Service = "System"; Description = "mDNS discovery"; Critical = $false }
    
    # Database Management
    "POST /super/database/cleanup" = @{ Service = "System"; Description = "Database cleanup"; Critical = $false }
    "GET /super/database/stats" = @{ Service = "System"; Description = "Database statistics"; Critical = $false }
}

# Define microservices endpoint mapping
$MicroservicesEndpoints = @{
    # Auth Service (through gateway: /api/auth/*)
    "POST /api/auth/login" = @{ Service = "Auth"; GatewayRoute = "/api/auth/login"; DirectUrl = "http://localhost:8081/auth/api/auth/login" }
    "POST /api/auth/refresh" = @{ Service = "Auth"; GatewayRoute = "/api/auth/refresh"; DirectUrl = "http://localhost:8081/auth/api/auth/refresh" }
    "POST /api/auth/super/login" = @{ Service = "Auth"; GatewayRoute = "/api/auth/super/login"; DirectUrl = "http://localhost:8081/auth/super/auth/login" }
    "POST /api/auth/super/refresh" = @{ Service = "Auth"; GatewayRoute = "/api/auth/super/refresh"; DirectUrl = "http://localhost:8081/auth/super/auth/refresh" }
    
    # Organization Service (through gateway: /api/organization/*)
    "GET /api/organization/entities" = @{ Service = "Organization"; GatewayRoute = "/api/organization/entities"; DirectUrl = "http://localhost:8082/organization/super/entities" }
    "POST /api/organization/entities" = @{ Service = "Organization"; GatewayRoute = "/api/organization/entities"; DirectUrl = "http://localhost:8082/organization/super/entities" }
    "GET /api/organization/permissions" = @{ Service = "Organization"; GatewayRoute = "/api/organization/permissions"; DirectUrl = "http://localhost:8082/organization/super/permissions" }
    
    # Subscriber Service (through gateway: /api/subscriber/*)
    "POST /api/subscriber/login" = @{ Service = "Subscriber"; GatewayRoute = "/api/subscriber/login"; DirectUrl = "http://localhost:8083/subscriber/subscriber/login" }
    "GET /api/subscriber/dashboard" = @{ Service = "Subscriber"; GatewayRoute = "/api/subscriber/dashboard"; DirectUrl = "http://localhost:8083/subscriber/subscriber/dashboard" }
    "GET /api/subscriber/subscribers" = @{ Service = "Subscriber"; GatewayRoute = "/api/subscriber/subscribers"; DirectUrl = "http://localhost:8083/subscriber/api/subscribers" }
    
    # Attendance Service (through gateway: /api/attendance/*)
    "GET /api/attendance/sessions" = @{ Service = "Attendance"; GatewayRoute = "/api/attendance/sessions"; DirectUrl = "http://localhost:8084/attendance/api/sessions" }
    "POST /api/attendance/sessions" = @{ Service = "Attendance"; GatewayRoute = "/api/attendance/sessions"; DirectUrl = "http://localhost:8084/attendance/api/sessions" }
    "POST /api/attendance/checkin/qr" = @{ Service = "Attendance"; GatewayRoute = "/api/attendance/checkin/qr"; DirectUrl = "http://localhost:8084/attendance/api/checkin/qr" }
    "POST /api/attendance/checkin/face" = @{ Service = "Attendance"; GatewayRoute = "/api/attendance/checkin/face"; DirectUrl = "http://localhost:8084/attendance/api/checkin/face" }
    "GET /api/attendance/reports" = @{ Service = "Attendance"; GatewayRoute = "/api/attendance/reports"; DirectUrl = "http://localhost:8084/attendance/api/reports" }
    
    # Menu Service (through gateway: /api/menu/*)
    "GET /api/menu/categories" = @{ Service = "Menu"; GatewayRoute = "/api/menu/categories"; DirectUrl = "http://localhost:8085/menu/api/menu/categories" }
    "GET /api/menu/items" = @{ Service = "Menu"; GatewayRoute = "/api/menu/items"; DirectUrl = "http://localhost:8085/menu/api/menu/items" }
    "GET /api/menu/public/categories" = @{ Service = "Menu"; GatewayRoute = "/api/menu/public/categories"; DirectUrl = "http://localhost:8085/menu/api/public/menu/categories" }
    "GET /api/menu/public/items" = @{ Service = "Menu"; GatewayRoute = "/api/menu/public/items"; DirectUrl = "http://localhost:8085/menu/api/public/menu/items" }
    
    # Order Service (through gateway: /api/order/*)
    "POST /api/order/orders" = @{ Service = "Order"; GatewayRoute = "/api/order/orders"; DirectUrl = "http://localhost:8086/order/api/orders" }
    "GET /api/order/orders" = @{ Service = "Order"; GatewayRoute = "/api/order/orders"; DirectUrl = "http://localhost:8086/order/api/orders" }
    
    # Table Service (through gateway: /api/table/*)
    "GET /api/table/tables" = @{ Service = "Table"; GatewayRoute = "/api/table/tables"; DirectUrl = "http://localhost:8087/table/api/tables" }
    "POST /api/table/tables" = @{ Service = "Table"; GatewayRoute = "/api/table/tables"; DirectUrl = "http://localhost:8087/table/api/tables" }
    "GET /api/table/tables/qr" = @{ Service = "Table"; GatewayRoute = "/api/table/tables/qr"; DirectUrl = "http://localhost:8087/table/api/tables/qr" }
}

# Initialize comparison results
$ComparisonResults = @{
    Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    MonolithicUrl = $MonolithicUrl
    GatewayUrl = $GatewayUrl
    TotalMonolithicEndpoints = $MonolithicEndpoints.Count
    TotalMicroservicesEndpoints = $MicroservicesEndpoints.Count
    MatchedEndpoints = @()
    MissingInMicroservices = @()
    ExtraInMicroservices = @()
    ServiceBreakdown = @{}
    Summary = @{}
}

Write-Info "`n=== Analyzing Endpoint Coverage ==="

# Analyze matched endpoints
foreach ($endpoint in $MonolithicEndpoints.GetEnumerator()) {
    $monolithicEndpoint = $endpoint.Key
    $monolithicInfo = $endpoint.Value
    
    if ($MicroservicesEndpoints.ContainsKey($monolithicEndpoint)) {
        $microserviceInfo = $MicroservicesEndpoints[$monolithicEndpoint]
        $ComparisonResults.MatchedEndpoints += @{
            Endpoint = $monolithicEndpoint
            Service = $monolithicInfo.Service
            Description = $monolithicInfo.Description
            Critical = $monolithicInfo.Critical
            GatewayRoute = $microserviceInfo.GatewayRoute
            DirectUrl = $microserviceInfo.DirectUrl
        }
        Write-Success "✅ MATCHED: $monolithicEndpoint"
    } else {
        $ComparisonResults.MissingInMicroservices += @{
            Endpoint = $monolithicEndpoint
            Service = $monolithicInfo.Service
            Description = $monolithicInfo.Description
            Critical = $monolithicInfo.Critical
        }
        if ($monolithicInfo.Critical) {
            Write-Error "❌ MISSING CRITICAL: $monolithicEndpoint"
        } else {
            Write-Warning "⚠️ MISSING: $monolithicEndpoint"
        }
    }
}

# Find extra endpoints in microservices
foreach ($endpoint in $MicroservicesEndpoints.GetEnumerator()) {
    $microserviceEndpoint = $endpoint.Key
    $microserviceInfo = $endpoint.Value
    
    if (-not $MonolithicEndpoints.ContainsKey($microserviceEndpoint)) {
        $ComparisonResults.ExtraInMicroservices += @{
            Endpoint = $microserviceEndpoint
            Service = $microserviceInfo.Service
            GatewayRoute = $microserviceInfo.GatewayRoute
            DirectUrl = $microserviceInfo.DirectUrl
        }
        Write-Info "➕ EXTRA: $microserviceEndpoint"
    }
}

# Generate service breakdown
$Services = @("Auth", "Organization", "Subscriber", "Attendance", "Menu", "Order", "Table", "File", "System")

foreach ($service in $Services) {
    $serviceMonolithicEndpoints = ($MonolithicEndpoints.Values | Where-Object { $_.Service -eq $service }).Count
    $serviceMatchedEndpoints = ($ComparisonResults.MatchedEndpoints | Where-Object { $_.Service -eq $service }).Count
    $serviceMissingEndpoints = ($ComparisonResults.MissingInMicroservices | Where-Object { $_.Service -eq $service }).Count
    
    $serviceCompletion = if ($serviceMonolithicEndpoints -gt 0) { 
        [math]::Round(($serviceMatchedEndpoints / $serviceMonolithicEndpoints) * 100, 2) 
    } else { 
        100 
    }
    
    $ComparisonResults.ServiceBreakdown[$service] = @{
        MonolithicEndpoints = $serviceMonolithicEndpoints
        MatchedEndpoints = $serviceMatchedEndpoints
        MissingEndpoints = $serviceMissingEndpoints
        CompletionPercentage = $serviceCompletion
    }
}

# Calculate overall statistics
$totalMatched = $ComparisonResults.MatchedEndpoints.Count
$totalMissing = $ComparisonResults.MissingInMicroservices.Count
$totalCriticalMissing = ($ComparisonResults.MissingInMicroservices | Where-Object { $_.Critical }).Count

$overallCompletion = [math]::Round(($totalMatched / $MonolithicEndpoints.Count) * 100, 2)
$criticalEndpointsTotal = ($MonolithicEndpoints.Values | Where-Object { $_.Critical }).Count
$criticalEndpointsMatched = ($ComparisonResults.MatchedEndpoints | Where-Object { $_.Critical }).Count
$criticalCompletion = if ($criticalEndpointsTotal -gt 0) { 
    [math]::Round(($criticalEndpointsMatched / $criticalEndpointsTotal) * 100, 2) 
} else { 
    100 
}

$ComparisonResults.Summary = @{
    OverallCompletion = $overallCompletion
    CriticalCompletion = $criticalCompletion
    TotalMatched = $totalMatched
    TotalMissing = $totalMissing
    TotalCriticalMissing = $totalCriticalMissing
    TotalExtra = $ComparisonResults.ExtraInMicroservices.Count
}

# Display summary
Write-Info "`n🎯 ENDPOINT COMPARISON SUMMARY"
Write-Info "=============================="
Write-Info "Total Monolithic Endpoints: $($MonolithicEndpoints.Count)"
Write-Info "Total Microservices Endpoints: $($MicroservicesEndpoints.Count)"
Write-Info "Matched Endpoints: $totalMatched"
Write-Info "Missing Endpoints: $totalMissing"
Write-Info "Critical Missing: $totalCriticalMissing"
Write-Info "Extra in Microservices: $($ComparisonResults.ExtraInMicroservices.Count)"
Write-Info ""
Write-Info "Overall Completion: $overallCompletion%"
Write-Info "Critical Endpoints Completion: $criticalCompletion%"

Write-Info "`n📊 SERVICE BREAKDOWN:"
foreach ($service in $Services) {
    $serviceData = $ComparisonResults.ServiceBreakdown[$service]
    if ($serviceData.MonolithicEndpoints -gt 0) {
        $status = if ($serviceData.CompletionPercentage -eq 100) { "✅" } 
                 elseif ($serviceData.CompletionPercentage -ge 80) { "⚠️" } 
                 else { "❌" }
        Write-Info "$status $service`: $($serviceData.MatchedEndpoints)/$($serviceData.MonolithicEndpoints) ($($serviceData.CompletionPercentage)%)"
    }
}

# Determine if 80% completion is achieved
if ($overallCompletion -ge 80 -and $criticalCompletion -ge 90) {
    Write-Success "`n🎉 MICROSERVICES TRANSITION 80%+ COMPLETE!"
    Write-Success "✅ Overall completion: $overallCompletion%"
    Write-Success "✅ Critical endpoints: $criticalCompletion%"
} elseif ($overallCompletion -ge 60) {
    Write-Warning "`n⚠️ Good progress, but needs more work to reach 80%"
    Write-Warning "Current completion: $overallCompletion%"
    Write-Warning "Critical endpoints: $criticalCompletion%"
} else {
    Write-Error "`n❌ Significant work needed to reach 80% completion"
    Write-Error "Current completion: $overallCompletion%"
    Write-Error "Critical endpoints: $criticalCompletion%"
}

# Save results to JSON file
try {
    $ComparisonResults | ConvertTo-Json -Depth 10 | Out-File -FilePath $OutputFile -Encoding UTF8
    Write-Success "`n✅ Comparison results saved to: $OutputFile"
} catch {
    Write-Error "❌ Failed to save results: $($_.Exception.Message)"
}

Write-Info "`n🚀 Analysis Complete!"
Write-Info "Next steps:"
Write-Info "1. Review missing critical endpoints"
Write-Info "2. Implement missing endpoints in respective microservices"
Write-Info "3. Update API Gateway routes for new endpoints"
Write-Info "4. Test all endpoints through the gateway"
