# Subscriber Android App - Development Setup

This guide explains how to set up automatic app running after each build for efficient development and testing.

## Quick Start Commands

### Option 1: Using Gradle Tasks (Recommended)

```bash
# Build, install and run the app automatically
./gradlew buildAndRun

# Or just install and run (if already built)
./gradlew runApp
```

### Option 2: Using PowerShell Script (Windows)

```powershell
# Run the PowerShell script
.\run-app.ps1
```

### Option 3: Using Batch Script (Windows)

```cmd
# Run the batch script
run-app.bat
```

## Available Gradle Tasks

The following custom Gradle tasks have been added to `app/build.gradle`:

### `runApp`
- **Description**: Install and run the debug version of the app
- **Dependencies**: `installDebug`
- **Usage**: `./gradlew runApp`
- **What it does**:
  1. Installs the debug APK to the connected device/emulator
  2. Starts the app automatically
  3. Shows success message

### `buildAndRun`
- **Description**: Build, install and run the debug version of the app
- **Dependencies**: `assembleDebug`, `runApp`
- **Usage**: `./gradlew buildAndRun`
- **What it does**:
  1. Builds the debug APK
  2. Installs it to the connected device/emulator
  3. Starts the app automatically

## Android Studio Setup

### Method 1: Using Gradle Tasks in Android Studio

1. Open the **Gradle** panel (usually on the right side)
2. Navigate to `subscriber-android > app > Tasks > application`
3. Double-click on `buildAndRun` or `runApp`
4. The app will build, install, and run automatically

### Method 2: Creating Custom Run Configuration

1. Go to **Run > Edit Configurations...**
2. Click the **+** button and select **Gradle**
3. Configure as follows:
   - **Name**: `Build and Run App`
   - **Gradle project**: `:app`
   - **Tasks**: `buildAndRun`
   - **Arguments**: (leave empty)
4. Click **OK**
5. Now you can select "Build and Run App" from the run configuration dropdown and click the run button

### Method 3: Adding to Toolbar

1. Right-click on the toolbar
2. Select **Customize Menus and Toolbars...**
3. Navigate to **Main Toolbar > Run Configurations**
4. Add your custom run configuration

## Prerequisites

### 1. Emulator/Device Setup
- Ensure an Android emulator is running or a physical device is connected
- Verify the device is detected: `adb devices`

### 2. Android SDK Configuration
- Make sure `local.properties` has the correct SDK path:
  ```properties
  sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
  ```

### 3. Backend Server
- Ensure the backend server is running on `http://localhost:8080`
- Test the health endpoint: `http://localhost:8080/subscriber/health`

## Testing the Wi-Fi Functionality

Once the app is running:

1. **Login** with test credentials:
   - Mobile Number: `9987654321`
   - PIN: `0000`

2. **Navigate to Wi-Fi Check-In**:
   - Tap the "WiFi Check-In" button on the dashboard

3. **Grant Permissions**:
   - Allow location permission when prompted (required for Wi-Fi network detection)

4. **Test Network Detection**:
   - The app will show the current Wi-Fi network
   - Signal strength indicator
   - Server connectivity status

5. **Test Check-In**:
   - When connected to an authorized network and server is reachable
   - The "Check In via WiFi" button will appear
   - Tap to perform Wi-Fi check-in

## Troubleshooting

### App doesn't start automatically
- Check if emulator/device is running: `adb devices`
- Verify the app is installed: `adb shell pm list packages | grep subscriberapp`
- Try manually starting: `adb shell am start -n com.example.subscriberapp/.ui.MainActivity`

### Build fails
- Clean and rebuild: `./gradlew clean assembleDebug`
- Check Android SDK path in `local.properties`
- Ensure all dependencies are downloaded

### Wi-Fi functionality issues
- Grant location permission in app settings
- Ensure backend server is running
- Check network connectivity between emulator and host

## Development Workflow

For efficient development:

1. Make code changes
2. Run `./gradlew buildAndRun`
3. App automatically builds, installs, and starts
4. Test the changes immediately
5. Repeat

This setup eliminates the need for manual APK installation and app launching, making the development cycle much faster and more efficient.
