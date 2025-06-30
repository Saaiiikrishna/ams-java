# 🚀 Microservices Deployment Guide

## 📋 **Overview**

This guide provides multiple ways to run the AMS microservices:
1. **Individual Service Execution** - Run any service independently
2. **Orchestrated Deployment** - Run all services together
3. **Development Mode** - With hot reload and debugging
4. **Production Mode** - Optimized for production

## 🛠️ **Prerequisites**

- ✅ Java 21+
- ✅ Maven 3.9+
- ✅ Docker & Docker Compose
- ✅ PostgreSQL (for local development)

## 🎯 **Deployment Options**

### **Option 1: Individual Service Execution (Native)**

#### **Step 1: Build All Services**
```powershell
# Build all services at once
.\scripts\build-all-services.ps1 -SkipTests

# Or build with tests
.\scripts\build-all-services.ps1

# Clean build
.\scripts\build-all-services.ps1 -Clean -SkipTests
```

#### **Step 2: Start Database (if needed)**
```powershell
# Start PostgreSQL
docker-compose -f infrastructure\docker-compose.individual.yml up -d postgres
```

#### **Step 3: Run Individual Services**
```powershell
# Run auth service with database
.\scripts\run-individual-service.ps1 -ServiceName auth-service -WithDatabase

# Run organization service
.\scripts\run-individual-service.ps1 -ServiceName organization-service -WithDatabase

# Run with debug mode
.\scripts\run-individual-service.ps1 -ServiceName auth-service -WithDatabase -Debug

# Run on custom port
.\scripts\run-individual-service.ps1 -ServiceName auth-service -Port 8090 -GrpcPort 9090
```

#### **Available Services:**
- `auth-service` (Port: 8081, gRPC: 9091)
- `organization-service` (Port: 8082, gRPC: 9092)
- `subscriber-service` (Port: 8083, gRPC: 9093)
- `attendance-service` (Port: 8084, gRPC: 9094)
- `menu-service` (Port: 8085, gRPC: 9095)
- `order-service` (Port: 8086, gRPC: 9096)
- `table-service` (Port: 8087, gRPC: 9097)
- `api-gateway` (Port: 8080)

### **Option 2: Individual Service Execution (Docker)**

#### **Step 1: Build Services**
```powershell
.\scripts\build-all-services.ps1 -SkipTests
```

#### **Step 2: Run Individual Service with Docker**
```bash
# Run auth service only
docker-compose -f infrastructure/docker-compose.individual.yml --profile auth-service up

# Run auth service with database
docker-compose -f infrastructure/docker-compose.individual.yml up postgres auth-service

# Run multiple specific services
docker-compose -f infrastructure/docker-compose.individual.yml up postgres auth-service organization-service
```

### **Option 3: All Services Together (Docker)**

#### **Step 1: Build All Services**
```powershell
.\scripts\build-all-services.ps1 -SkipTests
```

#### **Step 2: Run All Services**
```bash
# Run all microservices
docker-compose -f infrastructure/docker-compose.individual.yml --profile all-services up

# Run in background
docker-compose -f infrastructure/docker-compose.individual.yml --profile all-services up -d

# Build and run
docker-compose -f infrastructure/docker-compose.individual.yml --profile all-services up --build
```

### **Option 4: Original Docker Compose (Legacy)**

```bash
# Use the original docker-compose (if fixed)
docker-compose -f infrastructure/docker-compose.microservices.yml up --build
```

## 🔧 **Development Workflows**

### **Frontend Development**
```bash
# Start backend services
docker-compose -f infrastructure/docker-compose.individual.yml --profile all-services up -d

# Start frontend applications
cd frontend/admin-panel && npm start
cd frontend/entity-dashboard && npm start
cd frontend/public-menu && npm start
```

### **Backend Development**
```powershell
# Start database
docker-compose -f infrastructure/docker-compose.individual.yml up -d postgres

# Run service in development mode
.\scripts\run-individual-service.ps1 -ServiceName auth-service -WithDatabase -Profile dev

# Run with debugging
.\scripts\run-individual-service.ps1 -ServiceName auth-service -WithDatabase -Debug
```

### **Full Stack Development**
```bash
# Start all backend services
docker-compose -f infrastructure/docker-compose.individual.yml --profile all-services up -d

# Start frontend in development mode
cd frontend/admin-panel && npm start
```

## 🧪 **Testing Workflows**

### **Test Individual Service**
```powershell
# Test auth service
curl http://localhost:8081/auth/actuator/health

# Test through API Gateway
curl http://localhost:8080/api/auth/actuator/health
```

### **Run Comprehensive Tests**
```powershell
# Run the testing scripts
.\scripts\test-microservices-complete.ps1

# Compare endpoints
.\scripts\compare-endpoints.ps1
```

## 📊 **Monitoring & Debugging**

### **Check Service Status**
```bash
# Check running containers
docker ps

# Check service logs
docker logs ams-auth-service
docker logs ams-api-gateway

# Follow logs
docker logs -f ams-auth-service
```

### **Health Checks**
```bash
# Individual service health
curl http://localhost:8081/auth/actuator/health
curl http://localhost:8082/organization/actuator/health

# API Gateway health
curl http://localhost:8080/actuator/health
```

### **Database Access**
```bash
# Connect to PostgreSQL
docker exec -it ams-postgres psql -U postgres -d attendance_db

# Check tables
\dt

# Check data
SELECT * FROM super_admin;
```

## 🚨 **Troubleshooting**

### **Common Issues**

#### **Port Conflicts**
```bash
# Check what's using a port
netstat -tulpn | grep :8080

# Kill process using port
kill -9 $(lsof -t -i:8080)
```

#### **Database Connection Issues**
```bash
# Restart database
docker-compose -f infrastructure/docker-compose.individual.yml restart postgres

# Check database logs
docker logs ams-postgres
```

#### **Build Issues**
```powershell
# Clean build all services
.\scripts\build-all-services.ps1 -Clean -SkipTests

# Build shared library manually
cd backend/microservices/shared-lib
mvn clean install -DskipTests
```

#### **Docker Issues**
```bash
# Clean Docker
docker system prune -f

# Remove all containers
docker-compose -f infrastructure/docker-compose.individual.yml down

# Rebuild images
docker-compose -f infrastructure/docker-compose.individual.yml build --no-cache
```

## 📈 **Performance Tips**

### **Development**
- Use `-SkipTests` for faster builds
- Use `--profile` to run only needed services
- Use native execution for faster startup

### **Production**
- Use Docker for consistent environments
- Enable health checks
- Use resource limits
- Monitor with external tools

## 🎯 **Quick Commands Reference**

```bash
# Build everything
.\scripts\build-all-services.ps1 -SkipTests

# Run single service (native)
.\scripts\run-individual-service.ps1 -ServiceName auth-service -WithDatabase

# Run single service (Docker)
docker-compose -f infrastructure/docker-compose.individual.yml --profile auth-service up

# Run all services
docker-compose -f infrastructure/docker-compose.individual.yml --profile all-services up -d

# Test everything
.\scripts\test-microservices-complete.ps1

# Check status
docker ps
curl http://localhost:8080/actuator/health
```

## 🎉 **Success Indicators**

✅ **Individual Service Working:**
- Service starts without errors
- Health check returns 200 OK
- Database connection established
- gRPC port accessible

✅ **All Services Working:**
- All containers running
- API Gateway routing correctly
- Authentication flows working
- Database queries successful

✅ **Ready for Testing:**
- All services healthy
- Endpoints accessible through gateway
- Frontend can connect to backend
- Database populated with test data
