# PowerShell script to build and run the Subscriber Android App
Write-Host "Building and running Subscriber Android App..." -ForegroundColor Green
Write-Host ""

# Change to script directory
Set-Location $PSScriptRoot

# Read Android SDK path from local.properties
$localPropsPath = "local.properties"
$sdkDir = $null
if (Test-Path $localPropsPath) {
    $content = Get-Content $localPropsPath
    foreach ($line in $content) {
        if ($line -match "sdk\.dir=(.+)") {
            $sdkDir = $matches[1] -replace "\\\\", "\" -replace "\\:", ":"
            Write-Host "Found SDK path: $sdkDir" -ForegroundColor Cyan
            break
        }
    }
}

if (-not $sdkDir) {
    Write-Host "SDK path not found in local.properties" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

if (-not (Test-Path $sdkDir)) {
    Write-Host "Android SDK directory not found at: $sdkDir" -ForegroundColor Red
    Write-Host "Please update local.properties with correct sdk.dir path." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

$adbPath = Join-Path $sdkDir "platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    Write-Host "ADB not found at: $adbPath" -ForegroundColor Red
    Write-Host "Please ensure Android SDK platform-tools are installed." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}
Write-Host "Found ADB at: $adbPath" -ForegroundColor Cyan

Write-Host "Step 1: Building the app..." -ForegroundColor Yellow
& .\gradlew assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "Step 2: Installing the app..." -ForegroundColor Yellow
& .\gradlew installDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "Install failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "Step 3: Starting the app..." -ForegroundColor Yellow
Write-Host "Using ADB at: $adbPath" -ForegroundColor Cyan
& $adbPath shell am start -n com.example.subscriberapp/.ui.MainActivity
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to start app!" -ForegroundColor Red
    Write-Host "Make sure the emulator is running and the app is installed." -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "App started successfully!" -ForegroundColor Green
Write-Host "You can now test the Wi-Fi functionality in the emulator." -ForegroundColor Cyan
Read-Host "Press Enter to exit"
