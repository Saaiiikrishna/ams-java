# AGENTS Guidelines

This repository contains a Spring Boot backend and two native mobile apps.
The Android client lives in `entity-admin-android` and uses Kotlin.
The iOS client lives in `entity-admin-ios` and is built with SwiftUI.

## Development Rules

1. Keep mobile apps native. Do not introduce cross-platform frameworks.
2. After changes run `./mvnw -q test` when possible and report the result.
3. Commit all modifications using Git with clear messages.
4. Document significant updates in `README.md` or relevant `docs/` files.
5. When dependency downloads fail due to network limits, note this in testing.
6. Maintain separate directories for each platform and avoid mixing their code.

