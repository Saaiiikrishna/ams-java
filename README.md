# Attendance Management System

This project contains a Spring Boot backend, two React applications (admin panel and entity dashboard) and a small Python NFC client used for development.

## Requirements

- Java 17+
- Node.js 18+
- Maven 3.8+
- Python 3.10+ for the NFC client (with Tkinter available)

## Building the Backend

```bash
mvn clean package
```

The resulting JAR will be located in `target/` and can be run with `java -jar`.

## Frontend Apps

Each frontend is a separate React project. Install dependencies and build with:

```bash
cd admin-panel && npm install && npm run build
cd ../entity-dashboard && npm install && npm run build
```

During development you can run `npm start` in either directory to start the development server.

## Mobile Apps

Native mobile clients are provided in the `entity-admin-android` and `entity-admin-ios` directories.
The Android app is written in Kotlin and can be opened directly in Android Studio.
The iOS application uses SwiftUI and should be opened with Xcode.

The Android client uses Retrofit for networking and Hilt for dependency injection. It stores the JWT token securely using encrypted shared preferences, applies a consistent Material theme, and presents screens via the Jetpack Navigation component. After authentication it navigates to a session list retrieved from the backend.
The iOS client also features a login flow implemented in SwiftUI which persists the JWT token in the keychain and lists sessions using `URLSession`.

## Python NFC Client

`nfc_app.py` is a simple Tkinter application that can interact with the backend. Run it with Python after installing the `requests` package.

```bash
pip install requests
python nfc_app.py
```

## Docker Development

To start a local PostgreSQL instance required by the backend:

```bash
docker compose up -d db
```

The backend reads connection settings from environment variables defined in
`application.properties`.

Configuration such as API URL and JWT token is loaded from `nfc_config.json` if present (see the file for example values).

## Running Tests

Execute all unit and integration tests with:

```bash
mvn test
```

## Environment

The application uses an in-memory H2 database when running tests. For production you can configure another datasource in `application.properties`.

## API Documentation

Swagger UI is available after running the Spring Boot application. It provides an interactive interface to explore and test every endpoint.

- Start the backend with `mvn spring-boot:run` or by running the packaged JAR.
- Open <http://localhost:8080/swagger-ui.html> in your browser.

From this page you can execute requests, inspect request/response models and see example values for each operation.
