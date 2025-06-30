# Build All Microservices Script
# Builds shared library and all microservices in the correct order

param(
    [switch]$SkipTests,
    [switch]$Clean,
    [switch]$Parallel,
    [switch]$Verbose
)

# Color functions
function Write-Success { param($Message) Write-Host $Message -ForegroundColor Green }
function Write-Error { param($Message) Write-Host $Message -ForegroundColor Red }
function Write-Info { param($Message) Write-Host $Message -ForegroundColor Cyan }
function Write-Warning { param($Message) Write-Host $Message -ForegroundColor Yellow }

$services = @(
    "shared-lib",
    "auth-service", 
    "organization-service",
    "subscriber-service", 
    "attendance-service",
    "menu-service",
    "order-service", 
    "table-service",
    "api-gateway"
)

$basePath = Join-Path $PSScriptRoot ".." "backend" "microservices"
$buildResults = @{}
$totalStartTime = Get-Date

Write-Info "🚀 Building All Microservices"
Write-Info "=============================="
Write-Info "Base Path: $basePath"
Write-Info "Skip Tests: $SkipTests"
Write-Info "Clean Build: $Clean"
Write-Info "Parallel Build: $Parallel"
Write-Info ""

# Function to build a single service
function Build-Service {
    param(
        [string]$ServiceName,
        [string]$ServicePath
    )
    
    $startTime = Get-Date
    Write-Info "🔨 Building $ServiceName..."
    
    if (-not (Test-Path $ServicePath)) {
        Write-Error "❌ Service path not found: $ServicePath"
        return @{ Success = $false; Error = "Path not found"; Duration = 0 }
    }
    
    try {
        Push-Location $ServicePath
        
        $mvnArgs = @()
        if ($Clean) { $mvnArgs += "clean" }
        
        if ($ServiceName -eq "shared-lib") {
            $mvnArgs += "install"
        } else {
            $mvnArgs += "package"
        }
        
        if ($SkipTests) { $mvnArgs += "-DskipTests" }
        if (-not $Verbose) { $mvnArgs += "-q" }
        
        $mvnCommand = "mvn " + ($mvnArgs -join " ")
        Write-Info "   Command: $mvnCommand"
        
        $result = Invoke-Expression $mvnCommand
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -eq 0) {
            $duration = (Get-Date) - $startTime
            Write-Success "✅ $ServiceName built successfully ($($duration.TotalSeconds.ToString('F1'))s)"
            return @{ Success = $true; Duration = $duration.TotalSeconds }
        } else {
            Write-Error "❌ $ServiceName build failed (Exit code: $exitCode)"
            return @{ Success = $false; Error = "Build failed with exit code $exitCode"; Duration = 0 }
        }
    } catch {
        Write-Error "❌ $ServiceName build error: $($_.Exception.Message)"
        return @{ Success = $false; Error = $_.Exception.Message; Duration = 0 }
    } finally {
        Pop-Location
    }
}

# Build services
if ($Parallel -and $services.Count -gt 1) {
    Write-Info "🔄 Building services in parallel (except shared-lib)..."
    
    # Build shared-lib first (required by others)
    $sharedLibPath = Join-Path $basePath "shared-lib"
    $buildResults["shared-lib"] = Build-Service -ServiceName "shared-lib" -ServicePath $sharedLibPath
    
    if (-not $buildResults["shared-lib"].Success) {
        Write-Error "❌ Shared library build failed. Cannot continue."
        exit 1
    }
    
    # Build other services in parallel
    $otherServices = $services | Where-Object { $_ -ne "shared-lib" }
    $jobs = @()
    
    foreach ($service in $otherServices) {
        $servicePath = Join-Path $basePath $service
        $job = Start-Job -ScriptBlock {
            param($ServiceName, $ServicePath, $SkipTests, $Clean, $Verbose)
            
            # Re-define the function in the job scope
            function Build-Service {
                param([string]$ServiceName, [string]$ServicePath)
                
                if (-not (Test-Path $ServicePath)) {
                    return @{ Success = $false; Error = "Path not found"; Duration = 0 }
                }
                
                try {
                    Set-Location $ServicePath
                    
                    $mvnArgs = @()
                    if ($using:Clean) { $mvnArgs += "clean" }
                    $mvnArgs += "package"
                    if ($using:SkipTests) { $mvnArgs += "-DskipTests" }
                    if (-not $using:Verbose) { $mvnArgs += "-q" }
                    
                    $mvnCommand = "mvn " + ($mvnArgs -join " ")
                    $result = Invoke-Expression $mvnCommand
                    
                    if ($LASTEXITCODE -eq 0) {
                        return @{ Success = $true; Duration = 0 }
                    } else {
                        return @{ Success = $false; Error = "Build failed"; Duration = 0 }
                    }
                } catch {
                    return @{ Success = $false; Error = $_.Exception.Message; Duration = 0 }
                }
            }
            
            return Build-Service -ServiceName $ServiceName -ServicePath $ServicePath
        } -ArgumentList $service, $servicePath, $SkipTests, $Clean, $Verbose
        
        $jobs += @{ Service = $service; Job = $job }
    }
    
    # Wait for all jobs to complete
    Write-Info "⏳ Waiting for parallel builds to complete..."
    foreach ($jobInfo in $jobs) {
        $result = Receive-Job -Job $jobInfo.Job -Wait
        $buildResults[$jobInfo.Service] = $result
        Remove-Job -Job $jobInfo.Job
        
        if ($result.Success) {
            Write-Success "✅ $($jobInfo.Service) completed"
        } else {
            Write-Error "❌ $($jobInfo.Service) failed: $($result.Error)"
        }
    }
    
} else {
    Write-Info "🔄 Building services sequentially..."
    
    foreach ($service in $services) {
        $servicePath = Join-Path $basePath $service
        $buildResults[$service] = Build-Service -ServiceName $service -ServicePath $servicePath
        
        if (-not $buildResults[$service].Success) {
            Write-Error "❌ Build failed for $service. Stopping build process."
            break
        }
    }
}

# Generate summary
$totalDuration = (Get-Date) - $totalStartTime
$successCount = ($buildResults.Values | Where-Object { $_.Success }).Count
$failureCount = ($buildResults.Values | Where-Object { -not $_.Success }).Count

Write-Info "`n📊 BUILD SUMMARY"
Write-Info "================"
Write-Info "Total Duration: $($totalDuration.TotalSeconds.ToString('F1'))s"
Write-Info "Successful Builds: $successCount"
Write-Info "Failed Builds: $failureCount"
Write-Info ""

foreach ($service in $services) {
    $result = $buildResults[$service]
    if ($result.Success) {
        Write-Success "✅ $service"
    } else {
        Write-Error "❌ $service - $($result.Error)"
    }
}

if ($failureCount -eq 0) {
    Write-Success "`n🎉 All services built successfully!"
    Write-Info "You can now run individual services or use Docker Compose."
    Write-Info ""
    Write-Info "Next steps:"
    Write-Info "1. Run individual service: .\scripts\run-individual-service.ps1 -ServiceName auth-service"
    Write-Info "2. Run with Docker: docker-compose -f infrastructure\docker-compose.individual.yml --profile auth-service up"
    Write-Info "3. Run all services: docker-compose -f infrastructure\docker-compose.individual.yml --profile all-services up"
} else {
    Write-Error "`n❌ Some services failed to build. Please check the errors above."
    exit 1
}

Write-Info "`n🏁 Build process completed!"
