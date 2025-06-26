# PowerShell script to fix package imports in all microservices
# This script updates package declarations and imports in copied files

Write-Host "Fixing package imports in all microservices..." -ForegroundColor Green

# Define service mappings
$services = @{
    "auth" = @{
        "path" = "microservices/auth-service/src/main/java/com/example/attendancesystem/auth"
        "oldPackage" = "com.example.attendancesystem"
        "newPackage" = "com.example.attendancesystem.auth"
    }
    "organization" = @{
        "path" = "microservices/organization-service/src/main/java/com/example/attendancesystem/organization"
        "oldPackage" = "com.example.attendancesystem"
        "newPackage" = "com.example.attendancesystem.organization"
    }
    "subscriber" = @{
        "path" = "microservices/subscriber-service/src/main/java/com/example/attendancesystem/subscriber"
        "oldPackage" = "com.example.attendancesystem"
        "newPackage" = "com.example.attendancesystem.subscriber"
    }
    "attendance" = @{
        "path" = "microservices/attendance-service/src/main/java/com/example/attendancesystem/attendance"
        "oldPackage" = "com.example.attendancesystem"
        "newPackage" = "com.example.attendancesystem.attendance"
    }
    "menu" = @{
        "path" = "microservices/menu-service/src/main/java/com/example/attendancesystem/menu"
        "oldPackage" = "com.example.attendancesystem"
        "newPackage" = "com.example.attendancesystem.menu"
    }
    "order" = @{
        "path" = "microservices/order-service/src/main/java/com/example/attendancesystem/order"
        "oldPackage" = "com.example.attendancesystem"
        "newPackage" = "com.example.attendancesystem.order"
    }
    "table" = @{
        "path" = "microservices/table-service/src/main/java/com/example/attendancesystem/table"
        "oldPackage" = "com.example.attendancesystem"
        "newPackage" = "com.example.attendancesystem.table"
    }
}

function Fix-PackageImports {
    param(
        [string]$ServicePath,
        [string]$OldPackage,
        [string]$NewPackage,
        [string]$ServiceName
    )
    
    Write-Host "Fixing $ServiceName Service packages..." -ForegroundColor Yellow
    
    if (Test-Path $ServicePath) {
        # Get all Java files recursively
        $javaFiles = Get-ChildItem -Path $ServicePath -Filter "*.java" -Recurse
        
        foreach ($file in $javaFiles) {
            $content = Get-Content $file.FullName -Raw
            $modified = $false
            
            # Fix package declarations
            $subFolder = Split-Path (Split-Path $file.FullName -Parent) -Leaf
            
            switch ($subFolder) {
                "model" {
                    $newContent = $content -replace "package $OldPackage\.model", "package $NewPackage.model"
                    if ($newContent -ne $content) { $modified = $true; $content = $newContent }
                }
                "repository" {
                    $newContent = $content -replace "package $OldPackage\.repository", "package $NewPackage.repository"
                    if ($newContent -ne $content) { $modified = $true; $content = $newContent }
                }
                "service" {
                    $newContent = $content -replace "package $OldPackage\.service", "package $NewPackage.service"
                    if ($newContent -ne $content) { $modified = $true; $content = $newContent }
                }
                "controller" {
                    $newContent = $content -replace "package $OldPackage\.controller", "package $NewPackage.controller"
                    if ($newContent -ne $content) { $modified = $true; $content = $newContent }
                }
                "grpc" {
                    $newContent = $content -replace "package $OldPackage\.grpc\.service", "package $NewPackage.grpc"
                    if ($newContent -ne $content) { $modified = $true; $content = $newContent }
                }
                "security" {
                    $newContent = $content -replace "package $OldPackage\.security", "package $NewPackage.security"
                    if ($newContent -ne $content) { $modified = $true; $content = $newContent }
                }
                "facerecognition" {
                    $newContent = $content -replace "package $OldPackage\.facerecognition", "package $NewPackage.facerecognition"
                    if ($newContent -ne $content) { $modified = $true; $content = $newContent }
                }
            }
            
            # Fix imports - update to use service-specific packages
            $importReplacements = @{
                "import $OldPackage\.model\." = "import $NewPackage.model."
                "import $OldPackage\.repository\." = "import $NewPackage.repository."
                "import $OldPackage\.service\." = "import $NewPackage.service."
                "import $OldPackage\.controller\." = "import $NewPackage.controller."
                "import $OldPackage\.security\." = "import $NewPackage.security."
                "import $OldPackage\.grpc\.service\." = "import $NewPackage.grpc."
                "import $OldPackage\.facerecognition\." = "import $NewPackage.facerecognition."
            }
            
            foreach ($oldImport in $importReplacements.Keys) {
                $newImport = $importReplacements[$oldImport]
                $newContent = $content -replace [regex]::Escape($oldImport), $newImport
                if ($newContent -ne $content) { $modified = $true; $content = $newContent }
            }
            
            # Add shared library imports where needed
            if ($content -match "BaseEntity" -and $content -notmatch "import com\.example\.attendancesystem\.shared\.model\.BaseEntity") {
                $content = $content -replace "(package [^;]+;)", "`$1`n`nimport com.example.attendancesystem.shared.model.BaseEntity;"
                $modified = $true
            }
            
            if ($content -match "ApiResponse" -and $content -notmatch "import com\.example\.attendancesystem\.shared\.dto\.ApiResponse") {
                $content = $content -replace "(package [^;]+;)", "`$1`n`nimport com.example.attendancesystem.shared.dto.ApiResponse;"
                $modified = $true
            }
            
            if ($content -match "GrpcUtils" -and $content -notmatch "import com\.example\.attendancesystem\.shared\.util\.GrpcUtils") {
                $content = $content -replace "(package [^;]+;)", "`$1`n`nimport com.example.attendancesystem.shared.util.GrpcUtils;"
                $modified = $true
            }
            
            # Save the file if modified
            if ($modified) {
                Set-Content -Path $file.FullName -Value $content -NoNewline
                Write-Host "  Fixed: $($file.Name)" -ForegroundColor Gray
            }
        }
        
        Write-Host "$ServiceName Service packages fixed." -ForegroundColor Green
    } else {
        Write-Host "Warning: $ServicePath not found" -ForegroundColor Red
    }
}

# Fix packages for each service
foreach ($serviceName in $services.Keys) {
    $serviceConfig = $services[$serviceName]
    Fix-PackageImports -ServicePath $serviceConfig.path -OldPackage $serviceConfig.oldPackage -NewPackage $serviceConfig.newPackage -ServiceName $serviceName
}

Write-Host "`nPackage import fixes completed for all services!" -ForegroundColor Green
Write-Host "`nSummary:" -ForegroundColor Cyan
foreach ($serviceName in $services.Keys) {
    Write-Host "- $serviceName Service: ✓ Fixed" -ForegroundColor Green
}

Write-Host "`nNext: Creating service-specific configurations..." -ForegroundColor Yellow
