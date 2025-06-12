@echo off
echo Building Android APK...

REM Check if Android SDK is available
if not exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    echo Error: Android SDK not found. Please set ANDROID_HOME environment variable.
    echo Example: set ANDROID_HOME=C:\Users\YourUsername\AppData\Local\Android\Sdk
    pause
    exit /b 1
)

REM Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java not found. Please install Java 8 or higher.
    pause
    exit /b 1
)

REM Try to use system Gradle first
gradle --version >nul 2>&1
if not errorlevel 1 (
    echo Using system Gradle...
    gradle assembleDebug
) else (
    echo System Gradle not found. Please install Gradle or use Android Studio to build.
    echo.
    echo Alternative: Open this project in Android Studio and build from there.
    echo.
    echo To install Gradle:
    echo 1. Download Gradle from https://gradle.org/releases/
    echo 2. Extract to a folder (e.g., C:\gradle)
    echo 3. Add C:\gradle\bin to your PATH environment variable
    echo 4. Restart command prompt and try again
    pause
    exit /b 1
)

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo ✓ APK built successfully!
    echo Location: app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo You can now install this APK on your Android device.
) else (
    echo.
    echo ✗ Build failed. Please check the error messages above.
)

pause
