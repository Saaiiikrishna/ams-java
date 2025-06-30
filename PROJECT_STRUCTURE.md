# AMS Project Structure Guide

## 📁 Project Organization

This document provides a comprehensive guide to the newly restructured AMS (Attendance Management System) project.

## 🏗️ Architecture Overview

The project follows a **multi-tier architecture** with clear separation between:
- **Backend Services** (Monolithic + Microservices)
- **Frontend Applications** (React-based web apps)
- **Mobile Applications** (Native Android & iOS)
- **Infrastructure** (Docker, Database, Monitoring)

## 📂 Detailed Folder Structure

### 🔧 Backend (`/backend/`)

#### Monolithic Backend (`/backend/monolithic/`)
- **Purpose:** Original Spring Boot application
- **Technology:** Java 17, Spring Boot, Maven
- **Structure:**
  ```
  backend/monolithic/
  ├── src/main/java/          # Java source code
  ├── src/main/resources/     # Configuration files
  ├── src/test/               # Unit tests
  ├── pom.xml                 # Maven dependencies
  ├── Dockerfile              # Docker configuration
  └── mvnw, mvnw.cmd         # Maven wrapper
  ```

#### Microservices (`/backend/microservices/`)
- **Purpose:** Distributed microservices architecture
- **Technology:** Java 17, Spring Boot, gRPC, Maven
- **Services:**
  - `shared-lib/` - Common models, DTOs, gRPC definitions
  - `api-gateway/` - API Gateway (Port: 8080)
  - `auth-service/` - Authentication & Authorization (Port: 8081)
  - `organization-service/` - Organization Management (Port: 8082)
  - `subscriber-service/` - Subscriber Management (Port: 8083)
  - `attendance-service/` - Attendance Tracking (Port: 8084)
  - `menu-service/` - Menu Management (Port: 8085)
  - `order-service/` - Order Processing (Port: 8086)
  - `table-service/` - Table Management (Port: 8087)

### 🌐 Frontend (`/frontend/`)

#### Admin Panel (`/frontend/admin-panel/`)
- **Purpose:** Super administrator interface
- **Technology:** React, TypeScript, Material-UI
- **Features:** System management, entity administration

#### Entity Dashboard (`/frontend/entity-dashboard/`)
- **Purpose:** Entity administrator interface
- **Technology:** React, TypeScript
- **Features:** Subscriber management, attendance tracking

#### Public Menu (`/frontend/public-menu/`)
- **Purpose:** Public-facing menu display
- **Technology:** React, TypeScript
- **Features:** Menu browsing, ordering interface

### 📱 Mobile (`/mobile/`)

#### Android Applications (`/mobile/android/`)
- **Technology:** Kotlin, Jetpack Compose
- **Apps:**
  - `entity-admin/` - Entity administrator mobile app
  - `subscriber/` - Subscriber mobile app

#### iOS Applications (`/mobile/ios/`)
- **Technology:** Swift, SwiftUI
- **Apps:**
  - `entity-admin/` - Entity administrator iOS app
  - `subscriber/` - Subscriber iOS app

### 🏗️ Infrastructure (`/infrastructure/`)

#### Docker Configurations (`/infrastructure/docker/`)
- **Purpose:** Container configurations
- **Contents:** Database, performance, monitoring setups

#### Database (`/infrastructure/database/`)
- **Purpose:** Database initialization and migrations
- **Technology:** PostgreSQL
- **Contents:** `init.sql` and migration scripts

#### Nginx (`/infrastructure/nginx/`)
- **Purpose:** Reverse proxy and load balancing
- **Contents:** Nginx configuration files

#### Monitoring (`/infrastructure/monitoring/`)
- **Purpose:** System monitoring and observability
- **Technology:** Prometheus, Grafana
- **Contents:** Monitoring configurations

#### Docker Compose Files
- `docker-compose.yml` - Main development environment
- `docker-compose.microservices.yml` - Microservices deployment
- `docker-compose.prod.yml` - Production deployment

### 🛠️ Scripts (`/scripts/`)
- **Purpose:** Build and deployment automation
- **Contents:**
  - `build-jni-linux.sh` - JNI build for Linux
  - `build-jni-windows.bat` - JNI build for Windows
  - `deploy-docker.sh` - Docker deployment
  - `deploy.sh` - Main deployment script
  - `monitor.sh` - Monitoring script

### 📚 Documentation (`/docs/`)
- **Purpose:** Project documentation
- **Contents:** API documentation, guides, specifications

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Node.js 16+
- Docker & Docker Compose
- Android Studio (for Android development)
- Xcode (for iOS development)

### Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ams-java
   ```

2. **Start the infrastructure**
   ```bash
   cd infrastructure
   docker-compose up -d
   ```

3. **Run backend services**
   ```bash
   # Monolithic (choose one)
   cd backend/monolithic
   ./mvnw spring-boot:run
   
   # OR Microservices
   cd infrastructure
   docker-compose -f docker-compose.microservices.yml up
   ```

4. **Run frontend applications**
   ```bash
   # Admin Panel
   cd frontend/admin-panel
   npm install && npm start
   
   # Entity Dashboard
   cd frontend/entity-dashboard
   npm install && npm start
   
   # Public Menu
   cd frontend/public-menu
   npm install && npm start
   ```

## 🔄 Development Workflow

### Backend Development
1. Choose between monolithic or microservices
2. Make changes in respective directories
3. Run tests: `./mvnw test`
4. Build: `./mvnw clean package`

### Frontend Development
1. Navigate to specific frontend app
2. Install dependencies: `npm install`
3. Start development server: `npm start`
4. Build for production: `npm run build`

### Mobile Development
1. Open project in respective IDE
2. Install dependencies
3. Run on emulator/device

## 📊 Port Allocation

| Service | Port | Purpose |
|---------|------|---------|
| API Gateway | 8080 | Main entry point |
| Auth Service | 8081 | Authentication |
| Organization Service | 8082 | Organization management |
| Subscriber Service | 8083 | Subscriber management |
| Attendance Service | 8084 | Attendance tracking |
| Menu Service | 8085 | Menu management |
| Order Service | 8086 | Order processing |
| Table Service | 8087 | Table management |
| Admin Panel | 3000 | Admin interface |
| Entity Dashboard | 3001 | Entity interface |
| Public Menu | 3002 | Public interface |
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache |

## 🤝 Contributing

1. Follow the established folder structure
2. Place new components in appropriate directories
3. Update documentation when adding new features
4. Follow naming conventions
5. Write tests for new functionality

## 📝 Notes

- All build artifacts have been moved to `mreview/` folder
- Original file structure is preserved for reference
- Infrastructure components are centralized for easier management
- Each application maintains its own configuration files
