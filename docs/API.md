# API Endpoints

This document summarizes the REST endpoints exposed by the Spring Boot backend. All responses are JSON encoded.

## Authentication

### `POST /admin/authenticate`
Authenticate a user using their username and password.
- **Body**: `{"username": "string", "password": "string"}`
- **Response**: `200 OK` with a payload containing an `accessToken` and `refreshToken`.

### `POST /admin/refresh-token`
Request a new access token using a previously issued refresh token.
- **Body**: `{"refreshToken": "string"}`
- **Response**: `200 OK` with a new `accessToken` and `refreshToken`.

## Organization Administration
Requires the `SUPER_ADMIN` role.

### `POST /admin/entities`
Create a new organization.
- **Body**: `OrganizationDto`
- **Response**: `201 Created` with the created organization object.

### `GET /admin/entities`
Retrieve all organizations.
- **Response**: `200 OK` with a list of organizations.

### `POST /admin/entity-admins`
Create an entity administrator account for an organization.
- **Body**: `EntityAdminDto`
- **Response**: `201 Created` with the new admin information.

## Entity Administration
All routes below require the `ENTITY_ADMIN` role.

### `POST /entity/subscribers`
Add a subscriber to your organization. Optionally attach an NFC card UID.
- **Body**: `SubscriberDto`
- **Response**: `201 Created` with the subscriber data.

### `GET /entity/subscribers`
Get all subscribers in your organization.
- **Response**: `200 OK` with an array of `SubscriberDto`.

### `PUT /entity/subscribers/{id}`
Update subscriber details or NFC card assignment.
- **Body**: `SubscriberDto`
- **Response**: `200 OK` with the updated subscriber.

### `DELETE /entity/subscribers/{id}`
Delete a subscriber and their NFC card association.
- **Response**: `200 OK` with a confirmation message.

### `POST /entity/sessions`
Create an attendance session for your organization.
- **Body**: `AttendanceSessionDto`
- **Response**: `201 Created` with the created session.

### `PUT /entity/sessions/{id}/end`
End an active session.
- **Response**: `200 OK` with the ended session data.

### `GET /entity/sessions`
List sessions for your organization.
- **Response**: `200 OK` with an array of session objects.

## NFC Scanning
Requires authentication.

### `POST /nfc/scan`
Record an NFC card scan. Handles check-in and check-out based on session state.
- **Body**: `NfcScanDto`
- **Response**: `201 Created` or `200 OK` with a status message.

## Reports
Requires the `ENTITY_ADMIN` role.

### `GET /reports/sessions/{sessionId}/absentees`
List subscribers who did not attend a session.
- **Response**: `200 OK` with an array of absent subscribers.

### `GET /reports/subscribers/{subscriberId}/attendance`
Retrieve attendance logs for a subscriber within an optional date range.
- **Query Params**: `startDate` and `endDate` in ISO format.
- **Response**: `200 OK` with an array of attendance logs.
