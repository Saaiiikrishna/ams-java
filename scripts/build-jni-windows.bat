@echo off
REM Build script for SeetaFace6 JNI on Windows

echo Building SeetaFace6 JNI for Windows...

REM Set paths
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set CPP_DIR=%PROJECT_ROOT%\src\main\cpp
set BUILD_DIR=%CPP_DIR%\build

REM Create build directory
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

REM Change to build directory
cd /d "%BUILD_DIR%"

REM Check for Visual Studio
where cl >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Visual Studio compiler not found. Please run this from Visual Studio Developer Command Prompt.
    echo Or install Visual Studio Build Tools.
    pause
    exit /b 1
)

REM Check for CMake
where cmake >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo CMake not found. Please install CMake and add it to PATH.
    pause
    exit /b 1
)

REM Configure with CMake
echo Configuring with CMake...
cmake .. -G "Visual Studio 16 2019" -A x64
if %ERRORLEVEL% neq 0 (
    echo CMake configuration failed.
    pause
    exit /b 1
)

REM Build the project
echo Building JNI library...
cmake --build . --config Release
if %ERRORLEVEL% neq 0 (
    echo Build failed.
    pause
    exit /b 1
)

echo Build completed successfully!
echo JNI library should be in: %PROJECT_ROOT%\src\main\resources\native\lib\windows\x64\

pause
