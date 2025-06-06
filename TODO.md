# Mobile App TODO

This checklist outlines features to implement for the native Android and iOS apps.

## Android (Kotlin)
- [x] Add login screen with form validation.
- [ ] Connect to backend `/admin/authenticate` using Retrofit.
- [ ] Store JWT token securely and attach to requests.
- [ ] Display a list of sessions retrieved from the backend.
- [ ] Implement navigation components with fragments.
- [ ] Integrate Hilt for dependency injection.
- [ ] Apply consistent theming and styles.
- [ ] Add unit tests for view models and networking layer.

## iOS (SwiftUI)
- [ ] Create login view that authenticates via `/admin/authenticate`.
- [ ] Use `URLSession` for network requests.
- [ ] Persist the JWT token in Keychain.
- [ ] Build screens to list and manage sessions.
- [ ] Organize navigation with `NavigationView` or tab views.
- [ ] Define data models and services for backend entities.
- [ ] Add custom styling matching organization branding.
- [ ] Write unit tests for networking and state management.

