# Project TODO

This document gathers outstanding tasks to make the Attendance Management System production ready. Items are unchecked even if partial implementations exist so they can be revisited.

## Backend
- [x] Introduce a role model and seed a `SUPER_ADMIN` user for admin endpoints.
- [x] Align authentication payloads so all clients send a `username` field.
- [x] Provide Docker or other container setup for PostgreSQL and environment variables.
- [x] Add database migrations (e.g. Flyway) and seed demo data.
- [x] Configure CI so `./mvnw test` runs successfully in a container.

## Admin Panel (React)
- [ ] Implement navigation guards and global error handling.
- [x] Add richer form validation and environment-based API URLs.
- [x] Document production build and deployment steps.

## Entity Dashboard (React)
- [ ] Display real-time attendance updates (WebSocket or polling).
- [ ] Add logout functionality and token refresh logic.

## Android (Kotlin)
- [ ] Consolidate navigation with Jetpack Navigation components.
- [x] Fix login request to use a `username` field.
- [ ] Add session creation and subscriber management screens.
- [ ] Provide logout options and detailed error messages.

## iOS (SwiftUI)
- [x] Update `APIClient` login to send `username` instead of `email`.
- [x] Apply custom styling with dark‑mode support.
- [ ] Mirror Android features such as session management and logout.

## NFC Python Module
- [x] Integrate with real NFC hardware using an appropriate library.
- [x] Implement a secure login or token provisioning flow for the device.

## Cross‑module
- [x] Unify token storage format and authentication flows across all clients.
- [x] Supply Docker Compose to run backend, React apps and database together.
- [x] Set up CI that runs backend tests and builds all frontends.
