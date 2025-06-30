# Comprehensive Microservices Testing Script
# Tests all endpoints through API Gateway and individual services

param(
    [string]$GatewayUrl = "http://localhost:8080",
    [switch]$Verbose,
    [switch]$SkipAuth,
    [string]$OutputFile = "microservices-test-results.json"
)

# Color functions
function Write-Success { param($Message) Write-Host $Message -ForegroundColor Green }
function Write-Error { param($Message) Write-Host $Message -ForegroundColor Red }
function Write-Info { param($Message) Write-Host $Message -ForegroundColor Cyan }
function Write-Warning { param($Message) Write-Host $Message -ForegroundColor Yellow }

# Test results storage
$TestResults = @{
    Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    GatewayUrl = $GatewayUrl
    Infrastructure = @{}
    Services = @{}
    Gateway = @{}
    Authentication = @{}
    Endpoints = @{}
    Summary = @{}
}

Write-Info "🚀 Starting Comprehensive Microservices Testing"
Write-Info "Gateway URL: $GatewayUrl"
Write-Info "Output File: $OutputFile"

# Phase 1: Infrastructure Health Checks
Write-Info "`n=== Phase 1: Infrastructure Health Checks ==="

$InfrastructurePorts = @{
    "PostgreSQL" = 5432
    "Redis" = 6379
    "Zipkin" = 9411
    "Grafana" = 3003
    "Elasticsearch" = 9200
}

foreach ($service in $InfrastructurePorts.GetEnumerator()) {
    try {
        $connection = Test-NetConnection -ComputerName localhost -Port $service.Value -WarningAction SilentlyContinue
        if ($connection.TcpTestSucceeded) {
            Write-Success "✅ $($service.Key) (Port $($service.Value)): RUNNING"
            $TestResults.Infrastructure[$service.Key] = @{ Status = "RUNNING"; Port = $service.Value }
        } else {
            Write-Error "❌ $($service.Key) (Port $($service.Value)): NOT ACCESSIBLE"
            $TestResults.Infrastructure[$service.Key] = @{ Status = "NOT_ACCESSIBLE"; Port = $service.Value }
        }
    } catch {
        Write-Error "❌ $($service.Key): ERROR - $($_.Exception.Message)"
        $TestResults.Infrastructure[$service.Key] = @{ Status = "ERROR"; Error = $_.Exception.Message }
    }
}

# Phase 2: Microservices Health Checks
Write-Info "`n=== Phase 2: Microservices Health Checks ==="

$MicroservicesPorts = @{
    "API Gateway" = 8080
    "Auth Service" = 8081
    "Organization Service" = 8082
    "Subscriber Service" = 8083
    "Attendance Service" = 8084
    "Menu Service" = 8085
    "Order Service" = 8086
    "Table Service" = 8087
}

foreach ($service in $MicroservicesPorts.GetEnumerator()) {
    try {
        $connection = Test-NetConnection -ComputerName localhost -Port $service.Value -WarningAction SilentlyContinue
        if ($connection.TcpTestSucceeded) {
            Write-Success "✅ $($service.Key) (Port $($service.Value)): RUNNING"
            $TestResults.Services[$service.Key] = @{ Status = "RUNNING"; Port = $service.Value }
        } else {
            Write-Error "❌ $($service.Key) (Port $($service.Value)): NOT ACCESSIBLE"
            $TestResults.Services[$service.Key] = @{ Status = "NOT_ACCESSIBLE"; Port = $service.Value }
        }
    } catch {
        Write-Error "❌ $($service.Key): ERROR - $($_.Exception.Message)"
        $TestResults.Services[$service.Key] = @{ Status = "ERROR"; Error = $_.Exception.Message }
    }
}

# Phase 3: gRPC Ports Check
Write-Info "`n=== Phase 3: gRPC Ports Check ==="

$GrpcPorts = @{
    "Auth Service gRPC" = 9091
    "Organization Service gRPC" = 9092
    "Subscriber Service gRPC" = 9093
    "Attendance Service gRPC" = 9094
    "Menu Service gRPC" = 9095
    "Order Service gRPC" = 9096
    "Table Service gRPC" = 9097
}

foreach ($service in $GrpcPorts.GetEnumerator()) {
    try {
        $connection = Test-NetConnection -ComputerName localhost -Port $service.Value -WarningAction SilentlyContinue
        if ($connection.TcpTestSucceeded) {
            Write-Success "✅ $($service.Key) (Port $($service.Value)): OPEN"
            $TestResults.Services[$service.Key] = @{ Status = "OPEN"; Port = $service.Value }
        } else {
            Write-Error "❌ $($service.Key) (Port $($service.Value)): CLOSED"
            $TestResults.Services[$service.Key] = @{ Status = "CLOSED"; Port = $service.Value }
        }
    } catch {
        Write-Error "❌ $($service.Key): ERROR - $($_.Exception.Message)"
        $TestResults.Services[$service.Key] = @{ Status = "ERROR"; Error = $_.Exception.Message }
    }
}

# Phase 4: API Gateway Health
Write-Info "`n=== Phase 4: API Gateway Health ==="

try {
    $gatewayHealth = Invoke-RestMethod -Uri "$GatewayUrl/actuator/health" -Method GET -UseBasicParsing
    Write-Success "✅ API Gateway Health: $($gatewayHealth.status)"
    $TestResults.Gateway.Health = @{ Status = $gatewayHealth.status; Details = $gatewayHealth }
} catch {
    Write-Error "❌ API Gateway Health Check Failed: $($_.Exception.Message)"
    $TestResults.Gateway.Health = @{ Status = "ERROR"; Error = $_.Exception.Message }
}

# Phase 5: Service Health Endpoints
Write-Info "`n=== Phase 5: Service Health Endpoints ==="

$ServiceHealthEndpoints = @{
    "Auth Service" = "$GatewayUrl/api/auth/actuator/health"
    "Organization Service" = "$GatewayUrl/api/organization/actuator/health"
    "Subscriber Service" = "$GatewayUrl/api/subscriber/actuator/health"
    "Attendance Service" = "$GatewayUrl/api/attendance/actuator/health"
    "Menu Service" = "$GatewayUrl/api/menu/actuator/health"
    "Order Service" = "$GatewayUrl/api/order/actuator/health"
    "Table Service" = "$GatewayUrl/api/table/actuator/health"
}

foreach ($endpoint in $ServiceHealthEndpoints.GetEnumerator()) {
    try {
        $response = Invoke-RestMethod -Uri $endpoint.Value -Method GET -UseBasicParsing -TimeoutSec 10
        Write-Success "✅ $($endpoint.Key) Health: $($response.status)"
        $TestResults.Gateway[$endpoint.Key] = @{ Status = $response.status; Health = $response }
    } catch {
        Write-Error "❌ $($endpoint.Key) Health Failed: $($_.Exception.Message)"
        $TestResults.Gateway[$endpoint.Key] = @{ Status = "ERROR"; Error = $_.Exception.Message }
    }
}

# Phase 6: Authentication Testing (if not skipped)
if (-not $SkipAuth) {
    Write-Info "`n=== Phase 6: Authentication Testing ==="
    
    # Test Super Admin Login
    Write-Info "Testing Super Admin Authentication..."
    try {
        $superAdminLogin = @{
            username = "superadmin"
            password = "superadmin123"
        } | ConvertTo-Json
        
        $superAdminResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/auth/super/login" -Method POST -Body $superAdminLogin -ContentType "application/json" -UseBasicParsing
        Write-Success "✅ Super Admin Login: SUCCESS"
        $TestResults.Authentication.SuperAdmin = @{ Status = "SUCCESS"; HasToken = ($null -ne $superAdminResponse.accessToken) }
        $superAdminToken = $superAdminResponse.accessToken
    } catch {
        Write-Error "❌ Super Admin Login Failed: $($_.Exception.Message)"
        $TestResults.Authentication.SuperAdmin = @{ Status = "FAILED"; Error = $_.Exception.Message }
    }
    
    # Test Entity Admin Login
    Write-Info "Testing Entity Admin Authentication..."
    try {
        $entityAdminLogin = @{
            username = "admin"
            password = "admin123"
        } | ConvertTo-Json
        
        $entityAdminResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/auth/login" -Method POST -Body $entityAdminLogin -ContentType "application/json" -UseBasicParsing
        Write-Success "✅ Entity Admin Login: SUCCESS"
        $TestResults.Authentication.EntityAdmin = @{ Status = "SUCCESS"; HasToken = ($null -ne $entityAdminResponse.accessToken) }
        $entityAdminToken = $entityAdminResponse.accessToken
    } catch {
        Write-Error "❌ Entity Admin Login Failed: $($_.Exception.Message)"
        $TestResults.Authentication.EntityAdmin = @{ Status = "FAILED"; Error = $_.Exception.Message }
    }
}

# Phase 7: Endpoint Testing
Write-Info "`n=== Phase 7: Endpoint Testing ==="

$EndpointsToTest = @{
    # Public endpoints (no auth required)
    "Public Menu Categories" = @{ Url = "$GatewayUrl/api/menu/public/categories"; Method = "GET"; RequiresAuth = $false }
    "Public Menu Items" = @{ Url = "$GatewayUrl/api/menu/public/items"; Method = "GET"; RequiresAuth = $false }
    
    # Protected endpoints (require auth)
    "Organization Entities" = @{ Url = "$GatewayUrl/api/organization/entities"; Method = "GET"; RequiresAuth = $true; Role = "SuperAdmin" }
    "Menu Categories" = @{ Url = "$GatewayUrl/api/menu/categories"; Method = "GET"; RequiresAuth = $true; Role = "EntityAdmin" }
    "Menu Items" = @{ Url = "$GatewayUrl/api/menu/items"; Method = "GET"; RequiresAuth = $true; Role = "EntityAdmin" }
    "Attendance Sessions" = @{ Url = "$GatewayUrl/api/attendance/sessions"; Method = "GET"; RequiresAuth = $true; Role = "EntityAdmin" }
    "Tables List" = @{ Url = "$GatewayUrl/api/table/list"; Method = "GET"; RequiresAuth = $true; Role = "EntityAdmin" }
}

foreach ($endpoint in $EndpointsToTest.GetEnumerator()) {
    $endpointName = $endpoint.Key
    $endpointConfig = $endpoint.Value
    
    try {
        $headers = @{}
        
        # Add authorization header if required
        if ($endpointConfig.RequiresAuth -and -not $SkipAuth) {
            if ($endpointConfig.Role -eq "SuperAdmin" -and $superAdminToken) {
                $headers["Authorization"] = "Bearer $superAdminToken"
            } elseif ($endpointConfig.Role -eq "EntityAdmin" -and $entityAdminToken) {
                $headers["Authorization"] = "Bearer $entityAdminToken"
            } else {
                Write-Warning "⚠️ $endpointName: Skipped (No valid token)"
                $TestResults.Endpoints[$endpointName] = @{ Status = "SKIPPED"; Reason = "No valid token" }
                continue
            }
        }
        
        $response = Invoke-WebRequest -Uri $endpointConfig.Url -Method $endpointConfig.Method -Headers $headers -UseBasicParsing -TimeoutSec 10
        Write-Success "✅ $endpointName: $($response.StatusCode)"
        $TestResults.Endpoints[$endpointName] = @{ Status = "SUCCESS"; StatusCode = $response.StatusCode; ResponseLength = $response.Content.Length }
        
    } catch {
        $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode } else { "N/A" }
        Write-Error "❌ $endpointName: $statusCode - $($_.Exception.Message)"
        $TestResults.Endpoints[$endpointName] = @{ Status = "FAILED"; StatusCode = $statusCode; Error = $_.Exception.Message }
    }
}

# Phase 8: Generate Summary
Write-Info "`n=== Phase 8: Test Summary ==="

$totalInfrastructure = $TestResults.Infrastructure.Count
$runningInfrastructure = ($TestResults.Infrastructure.Values | Where-Object { $_.Status -eq "RUNNING" }).Count

$totalServices = $TestResults.Services.Count
$runningServices = ($TestResults.Services.Values | Where-Object { $_.Status -eq "RUNNING" -or $_.Status -eq "OPEN" }).Count

$totalEndpoints = $TestResults.Endpoints.Count
$successfulEndpoints = ($TestResults.Endpoints.Values | Where-Object { $_.Status -eq "SUCCESS" }).Count

$TestResults.Summary = @{
    Infrastructure = @{
        Total = $totalInfrastructure
        Running = $runningInfrastructure
        Percentage = if ($totalInfrastructure -gt 0) { [math]::Round(($runningInfrastructure / $totalInfrastructure) * 100, 2) } else { 0 }
    }
    Services = @{
        Total = $totalServices
        Running = $runningServices
        Percentage = if ($totalServices -gt 0) { [math]::Round(($runningServices / $totalServices) * 100, 2) } else { 0 }
    }
    Endpoints = @{
        Total = $totalEndpoints
        Successful = $successfulEndpoints
        Percentage = if ($totalEndpoints -gt 0) { [math]::Round(($successfulEndpoints / $totalEndpoints) * 100, 2) } else { 0 }
    }
}

# Calculate overall completion percentage
$overallCompletion = [math]::Round((
    $TestResults.Summary.Infrastructure.Percentage * 0.3 +
    $TestResults.Summary.Services.Percentage * 0.4 +
    $TestResults.Summary.Endpoints.Percentage * 0.3
), 2)

$TestResults.Summary.OverallCompletion = $overallCompletion

Write-Info "`n🎯 MICROSERVICES TESTING RESULTS"
Write-Info "================================"
Write-Info "Infrastructure: $($TestResults.Summary.Infrastructure.Running)/$($TestResults.Summary.Infrastructure.Total) ($($TestResults.Summary.Infrastructure.Percentage)%)"
Write-Info "Services: $($TestResults.Summary.Services.Running)/$($TestResults.Summary.Services.Total) ($($TestResults.Summary.Services.Percentage)%)"
Write-Info "Endpoints: $($TestResults.Summary.Endpoints.Successful)/$($TestResults.Summary.Endpoints.Total) ($($TestResults.Summary.Endpoints.Percentage)%)"
Write-Info ""

if ($overallCompletion -ge 80) {
    Write-Success "🎉 OVERALL COMPLETION: $overallCompletion% - MICROSERVICES TRANSITION 80%+ COMPLETE!"
} elseif ($overallCompletion -ge 60) {
    Write-Warning "⚠️ OVERALL COMPLETION: $overallCompletion% - Good progress, needs some fixes"
} else {
    Write-Error "❌ OVERALL COMPLETION: $overallCompletion% - Significant issues need resolution"
}

# Save results to JSON file
try {
    $TestResults | ConvertTo-Json -Depth 10 | Out-File -FilePath $OutputFile -Encoding UTF8
    Write-Success "`n✅ Test results saved to: $OutputFile"
} catch {
    Write-Error "❌ Failed to save results: $($_.Exception.Message)"
}

Write-Info "`n🚀 Testing Complete!"
Write-Info "Next steps:"
Write-Info "1. Review failed services and endpoints"
Write-Info "2. Check Docker container logs for errors"
Write-Info "3. Verify database connectivity and migrations"
Write-Info "4. Test authentication flows manually if needed"
