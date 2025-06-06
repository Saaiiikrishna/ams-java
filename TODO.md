# Mobile App TODO

This checklist outlines features to implement for the native Android and iOS apps.

## Android (Kotlin)
- [x] Add login screen with form validation.
- [x] Connect to backend `/admin/authenticate` using Retrofit.
- [x] Store JWT token securely and attach to requests.
- [x] Display a list of sessions retrieved from the backend.
- [ ] Implement navigation components with fragments.
- [x] Integrate Hilt for dependency injection.
- [ ] Apply consistent theming and styles.
- [x] Add unit tests for view models and networking layer.

## iOS (SwiftUI)
- [x] Create login view that authenticates via `/admin/authenticate`.
- [x] Use `URLSession` for network requests.
- [x] Persist the JWT token in Keychain.
- [x] Build screens to list and manage sessions.
- [x] Organize navigation with `NavigationView` or tab views.
- [x] Define data models and services for backend entities.
- [ ] Add custom styling matching organization branding.
- [x] Write unit tests for networking and state management.

