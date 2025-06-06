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

## Python NFC Client

`nfc_app.py` is a simple Tkinter application that can interact with the backend. Run it with Python after installing the `requests` package.

```bash
pip install requests
python nfc_app.py
```

Configuration such as API URL and JWT token is loaded from `nfc_config.json` if present (see the file for example values).

## Running Tests

Execute all unit and integration tests with:

```bash
mvn test
```

## Environment

The application uses an in-memory H2 database when running tests. For production you can configure another datasource in `application.properties`.
