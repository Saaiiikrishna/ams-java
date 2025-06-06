# Project TODO

This document gathers outstanding tasks to make the Attendance Management System production ready. Items are unchecked even if partial implementations exist so they can be revisited.

## Backend
- [ ] Introduce a role model and seed a `SUPER_ADMIN` user for admin endpoints.
- [ ] Align authentication payloads so all clients send a `username` field.
- [ ] Provide Docker or other container setup for PostgreSQL and environment variables.
- [ ] Add database migrations (e.g. Flyway) and seed demo data.
- [ ] Configure CI so `./mvnw test` runs successfully in a container.

## Admin Panel (React)
- [ ] Implement navigation guards and global error handling.
- [ ] Add richer form validation and environment-based API URLs.
- [ ] Document production build and deployment steps.

## Entity Dashboard (React)
- [ ] Display real-time attendance updates (WebSocket or polling).
- [ ] Add logout functionality and token refresh logic.

## Android (Kotlin)
- [ ] Consolidate navigation with Jetpack Navigation components.
- [ ] Fix login request to use a `username` field.
- [ ] Add session creation and subscriber management screens.
- [ ] Provide logout options and detailed error messages.

## iOS (SwiftUI)
- [ ] Update `APIClient` login to send `username` instead of `email`.
- [ ] Apply custom styling with dark‑mode support.
- [ ] Mirror Android features such as session management and logout.

## NFC Python Module
- [ ] Integrate with real NFC hardware using an appropriate library.
- [ ] Implement a secure login or token provisioning flow for the device.

## Cross‑module
- [ ] Unify token storage format and authentication flows across all clients.
- [ ] Supply Docker Compose to run backend, React apps and database together.
- [ ] Set up CI that runs backend tests and builds all frontends.
