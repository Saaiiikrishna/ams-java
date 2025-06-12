# Subscriber iOS App

This is the iOS application for subscribers to check in to attendance sessions using QR codes, NFC, Bluetooth, and WiFi.

## Features

- **Authentication**: Login using mobile number + 4-digit PIN or OTP
- **Dashboard**: View entity information, active sessions, and attendance history
- **QR Code Scanning**: Use camera to scan QR codes for attendance
- **Session Management**: View available sessions and check-in/check-out
- **Attendance History**: View past attendance records
- **Profile Management**: View and update subscriber profile

## Prerequisites

- Xcode 15.0 or later
- iOS 16.0 or later
- Swift 5.9 or later
- macOS Ventura or later

## Setup Instructions

1. **Open the project**:
   - Open Xcode
   - Select "Open a project or file"
   - Navigate to the `subscriber-ios` folder and select `SubscriberApp.xcodeproj`

2. **Update API Base URL**:
   - Open `SubscriberApp/Services/APIService.swift`
   - Update the `baseURL` to point to your backend server:
     ```swift
     private let baseURL = "http://your-server-ip:8080/subscriber/"
     ```
   - For local development on simulator, use `http://localhost:8080/subscriber/`
   - For physical device, use your computer's IP address

3. **Configure signing**:
   - Select the project in Xcode navigator
   - Go to "Signing & Capabilities" tab
   - Select your development team
   - Ensure "Automatically manage signing" is checked

## Building the App

### Using Xcode
1. Open the project in Xcode
2. Select your target device (simulator or physical device)
3. Press Cmd+R to build and run the app

### Using Command Line
1. Open Terminal
2. Navigate to the `subscriber-ios` directory
3. Run the following commands:

   **Build for simulator:**
   ```bash
   xcodebuild -project SubscriberApp.xcodeproj -scheme SubscriberApp -destination 'platform=iOS Simulator,name=iPhone 15' build
   ```

   **Build for device:**
   ```bash
   xcodebuild -project SubscriberApp.xcodeproj -scheme SubscriberApp -destination 'platform=iOS,name=Your Device Name' build
   ```

## Creating Archive for Distribution

1. **Archive the app**:
   - In Xcode, select "Product" > "Archive"
   - Wait for the archive to complete

2. **Export the archive**:
   - In the Organizer window, select your archive
   - Click "Distribute App"
   - Choose distribution method (App Store, Ad Hoc, Enterprise, etc.)
   - Follow the prompts to export

## Testing

### Running Unit Tests
```bash
xcodebuild test -project SubscriberApp.xcodeproj -scheme SubscriberApp -destination 'platform=iOS Simulator,name=iPhone 15'
```

### Running UI Tests
```bash
xcodebuild test -project SubscriberApp.xcodeproj -scheme SubscriberApp -destination 'platform=iOS Simulator,name=iPhone 15' -only-testing:SubscriberAppUITests
```

## App Architecture

- **MVVM Pattern**: Uses ObservableObject and @Published for reactive UI
- **SwiftUI**: Modern declarative UI framework
- **Combine**: For reactive programming and API calls
- **AVFoundation**: For camera and QR code scanning
- **UserDefaults**: For local data persistence

## API Endpoints Used

- `POST /subscriber/send-otp` - Send OTP for authentication
- `POST /subscriber/verify-otp` - Verify OTP and login
- `POST /subscriber/login-pin` - Login with PIN
- `GET /subscriber/dashboard` - Get dashboard data
- `GET /subscriber/sessions` - Get available sessions
- `POST /subscriber/checkin/qr` - QR code check-in
- `GET /subscriber/attendance/history` - Get attendance history

## Permissions Required

- **Camera**: For QR code scanning
- **Location** (optional): For location-based features
- **Bluetooth** (optional): For Bluetooth proximity check-in
- **NFC** (optional): For NFC card scanning

## App Structure

```
SubscriberApp/
├── Models/
│   ├── Subscriber.swift
│   ├── Organization.swift
│   ├── Session.swift
│   └── LoginRequest.swift
├── ViewModels/
│   ├── AuthViewModel.swift
│   ├── CheckInViewModel.swift
│   └── DashboardViewModel.swift
├── Views/
│   ├── ContentView.swift
│   ├── LoginView.swift
│   ├── DashboardView.swift
│   ├── QRScannerView.swift
│   ├── AttendanceHistoryView.swift
│   └── ProfileView.swift
├── Services/
│   └── APIService.swift
└── Resources/
    ├── Assets.xcassets
    └── Info.plist
```

## Troubleshooting

### Common Issues

1. **Build errors**:
   - Clean build folder: Product > Clean Build Folder
   - Reset package caches: File > Packages > Reset Package Caches
   - Restart Xcode

2. **App crashes on startup**:
   - Check if backend server is running
   - Verify API base URL is correct
   - Check device logs in Xcode console

3. **Camera not working**:
   - Ensure camera permission is granted
   - Test on physical device (simulator camera may not work properly)
   - Check Info.plist for camera usage description

4. **Network requests failing**:
   - Check if backend server is accessible
   - For HTTPS, ensure certificates are valid
   - For HTTP, add App Transport Security exception in Info.plist

### Debug Mode

The app includes detailed logging for network requests and responses in debug builds. Check the Xcode console for detailed logs.

## Deep Linking

The app supports deep linking for QR code check-ins:
```
ams://checkin?qr=SESSION_QR_CODE
```

## Contributing

1. Follow iOS development best practices
2. Use Swift coding conventions
3. Write unit tests for new features
4. Update this README when adding new features

## License

This project is part of the Attendance Management System.
