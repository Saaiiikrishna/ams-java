# Subscriber Android App

This is the Android application for subscribers to check in to attendance sessions using QR codes, NFC, Bluetooth, and WiFi.

## Features

- **Authentication**: Login using mobile number + 4-digit PIN or OTP
- **Dashboard**: View entity information, active sessions, and attendance history
- **QR Code Scanning**: Use camera to scan QR codes for attendance
- **Session Management**: View available sessions and check-in/check-out
- **Attendance History**: View past attendance records
- **Profile Management**: View and update subscriber profile

## Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24 (Android 7.0) or higher
- Java 8 or higher
- Gradle 8.2 or higher

## Setup Instructions

1. **Update local.properties**:
   - Open `local.properties` file
   - Update the `sdk.dir` path to match your Android SDK installation:
     ```
     sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
     ```

2. **Update API Base URL**:
   - Open `app/src/main/java/com/example/subscriberapp/di/NetworkModule.kt`
   - Update the `BASE_URL` to point to your backend server:
     ```kotlin
     private const val BASE_URL = "http://your-server-ip:8080/subscriber/"
     ```
   - For local development, use `http://10.0.2.2:8080/subscriber/` (Android emulator)
   - For physical device, use your computer's IP address

## Building the App

### Using Android Studio
1. Open Android Studio
2. Select "Open an existing Android Studio project"
3. Navigate to the `subscriber-android` folder and select it
4. Wait for Gradle sync to complete
5. Click "Run" or press Ctrl+R to build and run the app

### Using Command Line
1. Open terminal/command prompt
2. Navigate to the `subscriber-android` directory
3. Run the following commands:

   **For Windows:**
   ```bash
   .\gradlew.bat assembleDebug
   ```

   **For macOS/Linux:**
   ```bash
   ./gradlew assembleDebug
   ```

4. The APK will be generated in `app/build/outputs/apk/debug/`

## Generating Release APK

1. **Create a keystore** (if you don't have one):
   ```bash
   keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Update app/build.gradle** with signing configuration:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file('path/to/my-release-key.keystore')
               storePassword 'your-store-password'
               keyAlias 'my-key-alias'
               keyPassword 'your-key-password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               minifyEnabled true
               proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
           }
       }
   }
   ```

3. **Build release APK**:
   ```bash
   ./gradlew assembleRelease
   ```

## Testing

### Running Unit Tests
```bash
./gradlew test
```

### Running Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## App Architecture

- **MVVM Pattern**: Uses ViewModel and LiveData/StateFlow for reactive UI
- **Dependency Injection**: Hilt for dependency management
- **Networking**: Retrofit with OkHttp for API calls
- **UI**: Jetpack Compose for modern Android UI
- **Navigation**: Jetpack Navigation Compose
- **Camera**: CameraX for QR code scanning
- **Permissions**: Accompanist Permissions for runtime permissions

## API Endpoints Used

- `POST /subscriber/send-otp` - Send OTP for authentication
- `POST /subscriber/verify-otp` - Verify OTP and login
- `POST /subscriber/login-pin` - Login with PIN
- `GET /subscriber/dashboard` - Get dashboard data
- `GET /subscriber/sessions` - Get available sessions
- `POST /subscriber/checkin/qr` - QR code check-in
- `GET /subscriber/attendance/history` - Get attendance history

## Permissions Required

- **CAMERA**: For QR code scanning
- **INTERNET**: For API communication
- **ACCESS_NETWORK_STATE**: For network status checking
- **ACCESS_FINE_LOCATION**: For location-based features (optional)
- **BLUETOOTH**: For Bluetooth proximity check-in (optional)
- **NFC**: For NFC card scanning (optional)

## Troubleshooting

### Common Issues

1. **Gradle sync failed**:
   - Check internet connection
   - Update Android Studio to latest version
   - Clear Gradle cache: `./gradlew clean`

2. **App crashes on startup**:
   - Check if backend server is running
   - Verify API base URL is correct
   - Check device logs in Android Studio

3. **Camera not working**:
   - Ensure camera permission is granted
   - Test on physical device (emulator camera may not work properly)

4. **Network requests failing**:
   - Check if backend server is accessible
   - For HTTPS, ensure certificates are valid
   - For HTTP, add `android:usesCleartextTraffic="true"` in AndroidManifest.xml

### Debug Mode

To enable debug logging, the app uses `HttpLoggingInterceptor` which logs all network requests and responses in debug builds.

## Contributing

1. Follow Android development best practices
2. Use Kotlin coding conventions
3. Write unit tests for new features
4. Update this README when adding new features

## License

This project is part of the Attendance Management System.
