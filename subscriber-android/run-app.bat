@echo off
echo Building and running Subscriber Android App...
echo.

cd /d "%~dp0"

echo Step 1: Building the app...
call gradlew assembleDebug
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Installing the app...
call gradlew installDebug
if %ERRORLEVEL% neq 0 (
    echo Install failed!
    pause
    exit /b 1
)

echo.
echo Step 3: Starting the app...
adb shell am start -n com.example.subscriberapp/.ui.MainActivity
if %ERRORLEVEL% neq 0 (
    echo Failed to start app!
    pause
    exit /b 1
)

echo.
echo App started successfully!
echo You can now test the Wi-Fi functionality in the emulator.
pause
