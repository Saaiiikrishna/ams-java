# Microservices Decomposition TODO List

## 7-Phase Microservices Decomposition Progress

### Phase 1: Build Verification ✅ COMPLETED
- [x] **Backend Build Verification**
  - [x] Clean and build Spring Boot backend successfully
  - [x] Verify all dependencies resolve correctly
  - [x] Ensure no compilation errors
  - [x] Test basic application startup

- [x] **Android Build Verification** 
  - [x] Clean and build Android subscriber app successfully
  - [x] Verify all dependencies resolve correctly
  - [x] Ensure no compilation errors
  - [x] Generate APK file successfully
  - [x] Copy APK to desktop (subscriber-app-debug.apk)

### Phase 2: Domain Separation with gRPC 🔄 IN PROGRESS
- [x] **Identify Domain Boundaries**
  - [x] Analyze current monolithic structure
  - [x] Identify core business domains (User Management, Menu Management, Order Processing, etc.)
  - [x] Map existing controllers and services to domains
  - [x] Define clear domain boundaries and responsibilities

**IDENTIFIED DOMAINS:**
1. **Authentication & Authorization Domain**
   - Controllers: AuthenticationController, SuperAdminAuthController, SubscriberController (auth endpoints)
   - Services: SubscriberAuthService, RefreshTokenService, SuperAdminRefreshTokenService
   - Models: EntityAdmin, SuperAdmin, SubscriberAuth, RefreshToken, SuperAdminRefreshToken, Role

2. **Organization Management Domain**
   - Controllers: SuperAdminController, EntityController (org management)
   - Services: PermissionService
   - Models: Organization, OrganizationPermission, FeaturePermission

3. **Subscriber Management Domain**
   - Controllers: EntityController (subscriber CRUD), SubscriberController (profile)
   - Services: SubscriberAuthService (profile management)
   - Models: Subscriber, NfcCard

4. **Attendance Management Domain**
   - Controllers: EntityController (sessions), SubscriberController (checkin/checkout)
   - Services: ScheduledSessionService, QrCodeService
   - Models: AttendanceSession, ScheduledSession, AttendanceLog, CheckInMethod

5. **Menu Management Domain**
   - Controllers: PublicMenuController
   - Services: MenuService
   - Models: Category, Item

6. **Order Management Domain**
   - Controllers: PublicMenuController (order endpoints)
   - Services: OrderService
   - Models: Order, OrderItem, OrderStatus

7. **Table Management Domain**
   - Controllers: PublicMenuController (table endpoints)
   - Services: TableService, TableQrCodeService
   - Models: RestaurantTable

8. **Infrastructure Services Domain**
   - Services: MDnsService, DJLFaceRecognitionService, DatabaseCleanupService
   - Cross-cutting concerns: Security, Configuration, Utilities

- [x] **Design gRPC Service Contracts** ✅ COMPLETED
  - [x] Create .proto files for each identified domain
  - [x] Define service interfaces and message types
  - [x] Establish inter-service communication patterns
  - [x] Design error handling and status codes

- [x] **Implement gRPC Services** ✅ BASIC IMPLEMENTATION COMPLETE
  - [x] Add gRPC dependencies to projects
  - [x] Configure protobuf-maven-plugin for code generation
  - [x] Generate Java classes from proto files (480+ classes generated)
  - [x] Fix javax.annotation.Generated compilation issues
  - [x] Set up gRPC server infrastructure with interceptors
  - [x] Implement basic gRPC service implementations (Auth + Organization)
  - [x] Configure gRPC server properties
  - [ ] Create gRPC client configurations
  - [ ] Implement remaining service implementations (Subscriber, Attendance, Menu, Order, Table)

- [ ] **Refactor Existing Code**
  - [ ] Extract domain-specific logic into separate modules
  - [ ] Replace direct method calls with gRPC calls
  - [ ] Update data access patterns for domain separation
  - [ ] Maintain backward compatibility during transition

### Phase 3: mDNS Service Discovery ✅ COMPLETED
- [x] **Backend mDNS Configuration**
  - [x] Implement mDNS service registration
  - [x] Configure service discovery mechanisms
  - [x] Set up health check endpoints
  - [x] Test service discovery functionality
  - [x] Create comprehensive service discovery manager
  - [x] Add gRPC client factory with automatic service discovery
  - [x] Implement REST API for monitoring discovered services

- [ ] **Android mDNS Integration**
  - [ ] Implement mDNS service discovery in Android app
  - [ ] Replace hardcoded IP addresses with dynamic discovery
  - [ ] Add fallback mechanisms for discovery failures
  - [ ] Test cross-network compatibility

### Phase 4: Dynamic IP Handling 🔄 PENDING
- [ ] **Network Adaptability**
  - [ ] Implement dynamic IP resolution
  - [ ] Handle network changes gracefully
  - [ ] Add connection retry mechanisms
  - [ ] Test on different network configurations

- [ ] **Configuration Management**
  - [ ] Remove hardcoded network configurations
  - [ ] Implement environment-specific configurations
  - [ ] Add runtime configuration updates
  - [ ] Test configuration changes without restarts

### Phase 5: Client-Side Discovery 🔄 PENDING
- [ ] **Android Client Enhancements**
  - [ ] Implement intelligent service discovery
  - [ ] Add service health monitoring
  - [ ] Implement load balancing for multiple service instances
  - [ ] Add circuit breaker patterns

- [ ] **Service Registry Integration**
  - [ ] Connect to service registry
  - [ ] Implement service metadata handling
  - [ ] Add service versioning support
  - [ ] Test service failover scenarios

### Phase 6: Resilience & Observability 🔄 PENDING
- [ ] **Resilience Patterns**
  - [ ] Implement circuit breakers
  - [ ] Add retry mechanisms with exponential backoff
  - [ ] Implement timeout handling
  - [ ] Add bulkhead isolation patterns

- [ ] **Observability**
  - [ ] Add distributed tracing
  - [ ] Implement metrics collection
  - [ ] Set up centralized logging
  - [ ] Create monitoring dashboards
  - [ ] Add health check endpoints

- [ ] **Error Handling**
  - [ ] Implement graceful degradation
  - [ ] Add comprehensive error reporting
  - [ ] Create error recovery mechanisms
  - [ ] Test failure scenarios

### Phase 7: Containerization 🔄 PENDING
- [ ] **Docker Configuration**
  - [ ] Create Dockerfiles for each service
  - [ ] Set up multi-stage builds
  - [ ] Optimize container sizes
  - [ ] Configure container networking

- [ ] **Orchestration**
  - [ ] Create Docker Compose configurations
  - [ ] Set up service dependencies
  - [ ] Configure volume mounts and networking
  - [ ] Test container deployment

- [ ] **Production Readiness**
  - [ ] Add container health checks
  - [ ] Configure resource limits
  - [ ] Set up container monitoring
  - [ ] Create deployment scripts

## Current Status
- **Current Phase**: Phase 3 (mDNS Service Discovery) - COMPLETED ✅
- **Last Completed**: Full mDNS service discovery infrastructure with gRPC integration
- **Next Milestone**: Phase 4 (Dynamic IP Handling) and Android mDNS integration

## Notes
- Phase 1 completed successfully with both backend and Android builds working
- APK file generated and copied to desktop: `subscriber-app-debug.apk`
- Ready to proceed with Phase 2: Domain separation and gRPC implementation
- All subsequent phases depend on successful completion of Phase 2

## Important Reminders
- Always check this file when in doubt about next steps
- Mark tasks as completed when finished: [x]
- Update current status after each major milestone
- Keep notes about any issues or decisions made during implementation
- Use the folder 'useless' to store all unwanted files. DO NOT DELETE / REMOVE ANY FILES. If you cannot find the folder 'useless', create it in the root directory of the project.
