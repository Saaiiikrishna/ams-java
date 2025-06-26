# Modules For Smart Home Automation

This project is a comprehensive Automation systems featuring a Spring Boot backend, three React-based frontend applications (Admin Panel, Entity Dashboard, and Public Menu), native mobile applications for both Android and iOS (for Entity Admins and Subscribers), and various development tools.

## Table of Contents

- [Project Overview](#project-overview)
- [Prerequisites](#prerequisites)
- [Backend Setup](#backend-setup)
  - [Building the Backend](#building-the-backend)
  - [Running the Backend](#running-the-backend)
- [Frontend Setup](#frontend-setup)
  - [Admin Panel](#admin-panel)
  - [Entity Dashboard](#entity-dashboard)
  - [Public Menu](#public-menu)
- [Mobile App Setup](#mobile-app-setup)
  - [Entity Admin Android App](#entity-admin-android-app)
  - [Entity Admin iOS App](#entity-admin-ios-app)
  - [Subscriber Android App](#subscriber-android-app)
  - [Subscriber iOS App](#subscriber-ios-app)
- [JNI and Face Recognition](#jni-and-face-recognition)
- [Python NFC Client (Development Tool)](#python-nfc-client-development-tool)
- [Docker Development](#docker-development)
- [Running Tests](#running-tests)
  - [Backend Tests](#backend-tests)
  - [Frontend Tests](#frontend-tests)
  - [Mobile App Tests](#mobile-app-tests)
- [API Documentation](#api-documentation)
- [Environment Configuration](#environment-configuration)
- [Contributing](#contributing)
- [License](#license)

## Project Overview

The system is composed of several key components:

*   **Backend:** A Java Spring Boot application responsible for the core business logic, API endpoints, and database interactions.
*   **Admin Panel:** A React (TypeScript) web application for super administrators to manage the system, entities, and other administrators.
*   **Entity Dashboard:** A React (TypeScript) web application for entity administrators to manage their specific entity's subscribers, sessions, attendance, and other entity-specific settings.
*   **Public Menu:** A React (TypeScript) web application likely used for displaying public-facing menus or information.
*   **Entity Admin Mobile Apps:** Native Android (Kotlin) and iOS (SwiftUI) applications for entity administrators, providing a mobile interface for management tasks.
*   **Subscriber Mobile Apps:** Native Android (Kotlin/Jetpack Compose) and iOS (SwiftUI) applications for subscribers to check in, view history, and manage their profiles.
*   **JNI/Face Recognition:** Includes Java Native Interface (JNI) components, potentially for integrating with native libraries like SeetaFace6 for face recognition functionalities. (See `src/main/jni/README.md` and `scripts/` for more details).

## Prerequisites

To build and run this project, you will need the following software installed:

*   **Java:** JDK 17 or newer
*   **Maven:** Version 3.8+ (for the backend)
*   **Node.js:** Version 18+ (for the frontend applications)
*   **npm:** Usually comes with Node.js
*   **Python:** Version 3.10+ (for the NFC client development tool)
*   **Docker & Docker Compose:** (Optional, for running PostgreSQL or other services)
*   **Android Studio:** For Android app development (latest stable version recommended)
*   **Xcode:** For iOS app development (latest stable version recommended, requires macOS)

## Backend Setup

The backend is a Spring Boot application.

### Building the Backend

Navigate to the project root directory and run:

```bash
mvn clean package
```

The compiled JAR file will be located in the `target/` directory.

### Running the Backend

You can run the backend application using:

```bash
java -jar target/your-application-name.jar # Replace with the actual JAR name
```

Alternatively, during development, you can run it directly using the Spring Boot Maven plugin:

```bash
mvn spring-boot:run
```

The backend typically requires a database. See the [Docker Development](#docker-development) section for setting up a PostgreSQL instance. Configuration for database connection and other properties can be found in `src/main/resources/application.properties`.

## Frontend Setup

The project includes three React-based frontend applications. For each frontend application, navigate to its respective directory (`admin-panel`, `entity-dashboard`, `public-menu`) and follow these general steps:

1.  **Install Dependencies:**
    ```bash
    npm install
    ```
2.  **Run in Development Mode:**
    ```bash
    npm start
    ```
    This will typically start a development server (e.g., on `http://localhost:3000`).
3.  **Build for Production:**
    ```bash
    npm run build
    ```
    This will create an optimized production build in the `build` folder (or a similar directory like `dist`).

Refer to the individual README files in each frontend directory for more specific details:
*   `admin-panel/README.md`
*   `entity-dashboard/README.md`
*   For `public-menu`, it uses standard `react-scripts` (see `public-menu/package.json`). Ensure you set any required environment variables (e.g., `REACT_APP_API_BASE_URL` in a `.env` file).

## Mobile App Setup

The project includes native mobile applications for both Android and iOS platforms, catering to Entity Administrators and Subscribers.

### Entity Admin Android App

*   **Location:** `entity-admin-android/`
*   **Language:** Kotlin
*   **Setup:** Open the `entity-admin-android` directory in Android Studio. Gradle will handle the build and dependency resolution.
*   **Details:** See `entity-admin-android/README.md`.

### Entity Admin iOS App

*   **Location:** `entity-admin-ios/`
*   **Language:** SwiftUI
*   **Setup:** Open the `EntityAdminIOS` project within the `entity-admin-ios` directory using Xcode.
*   **Details:** See `entity-admin-ios/README.md`.

### Subscriber Android App

*   **Location:** `subscriber-android/`
*   **Language:** Kotlin with Jetpack Compose
*   **Setup:**
    1.  Open the `subscriber-android` directory in Android Studio.
    2.  Update `local.properties` with your Android SDK path if necessary.
    3.  Configure the API base URL in `app/src/main/java/com/example/subscriberapp/di/NetworkModule.kt`.
*   **Details:** For detailed build, run, and test instructions, refer to `subscriber-android/README.md`.

### Subscriber iOS App

*   **Location:** `subscriber-ios/`
*   **Language:** SwiftUI
*   **Setup:**
    1.  Open `SubscriberApp.xcodeproj` from the `subscriber-ios` directory in Xcode.
    2.  Update the API base URL in `SubscriberApp/Services/APIService.swift`.
    3.  Configure signing in Xcode.
*   **Details:** For detailed build, run, and test instructions, refer to `subscriber-ios/README.md`.

## JNI and Face Recognition

The project utilizes Java Native Interface (JNI) for integrating with native libraries, potentially including SeetaFace6 for face recognition.

*   The primary JNI implementation can be found in `src/main/cpp/seetaface_jni.cpp`.
*   A mock JNI implementation for testing purposes is located in `src/main/jni/seetaface_jni_mock.cpp`. See `src/main/jni/README.md` for more information on this mock setup.
*   Various scripts related to building and setting up these components are available in the `scripts/` directory (e.g., `build-jni-linux.sh`, `setup-seetaface6.py`).

## Python NFC Client (Development Tool)

A simple Python Tkinter application (`nfc_app.py` - *Note: this file was mentioned in the original README but not visible in the initial `ls` output, assuming it might be in the root or scripts directory*) is provided for interacting with the backend, primarily for development and testing of NFC functionalities.

To run it:

1.  Install the `requests` package:
    ```bash
    pip install requests
    ```
2.  Run the script:
    ```bash
    python nfc_app.py # or python path/to/nfc_app.py
    ```
    Configuration such as API URL and JWT token might be loaded from `nfc_config.json` if present.

## Docker Development

To simplify local development, a Docker Compose setup can be used to run required services, such as a PostgreSQL database.

To start a local PostgreSQL instance:

```bash
docker compose up -d db
```

The backend reads database connection settings from environment variables, which can be overridden in `src/main/resources/application.properties` or via system environment variables.

## Running Tests

### Backend Tests

Execute all unit and integration tests for the Spring Boot backend using Maven:

```bash
mvn test
```
The application uses an in-memory H2 database when running tests by default.

### Frontend Tests

For each React frontend application (`admin-panel`, `entity-dashboard`, `public-menu`), navigate to its directory and run:

```bash
npm test
```
This typically launches the test runner in interactive watch mode.

### Mobile App Tests

*   **Android:** Refer to the `README.md` files in `entity-admin-android/` and `subscriber-android/` for instructions on running unit and instrumented tests (usually via Gradle commands like `./gradlew test` and `./gradlew connectedAndroidTest`).
*   **iOS:** Refer to the `README.md` files in `entity-admin-ios/` and `subscriber-ios/` for instructions on running tests using Xcode or `xcodebuild` command-line tools.

## API Documentation

Swagger UI is available for interactive API exploration and testing once the Spring Boot backend is running.

1.  Start the backend (e.g., `mvn spring-boot:run` or by running the packaged JAR).
2.  Open <http://localhost:8080/swagger-ui.html> in your browser (assuming the default port 8080).

From Swagger UI, you can execute requests, inspect request/response models, and see example values for each operation. Additional API documentation might be present in `docs/API.md`.

## Environment Configuration

*   **Backend:** Configuration is primarily managed via `src/main/resources/application.properties`. Environment variables can override these properties.
*   **Frontend:** React applications often use `.env` files for environment-specific variables (e.g., `REACT_APP_API_BASE_URL`). Refer to individual frontend READMEs and `package.json` files.
*   **Mobile Apps:** API endpoints and other configurations are typically set within specific source code files (e.g., `NetworkModule.kt` for Android, `APIService.swift` for iOS). See their respective READMEs.

## Contributing

We welcome contributions to the Automation System! Please see our [CONTRIBUTING.md](./CONTRIBUTING.md) file for guidelines on how to report bugs, suggest features, and submit pull requests.

You can also reach out with specific queries to [saaiiikrishna@gmail.com](mailto:saaiiikrishna@gmail.com).

## License

This project is open source and licensed under the MIT License. See the [LICENSE](./LICENSE) file for more details.
