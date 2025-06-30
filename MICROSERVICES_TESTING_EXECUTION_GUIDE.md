# 🚀 Microservices Testing Execution Guide

## 📋 **Prerequisites**

### **Required Software**
- ✅ Docker & Docker Compose
- ✅ PowerShell 5.1+ (Windows) or Bash (Linux/Mac)
- ✅ curl (for HTTP requests)
- ✅ PostgreSQL client (optional, for database verification)

### **Required Ports (must be free)**
- **Infrastructure**: 5432 (PostgreSQL), 6379 (Redis), 9411 (Zipkin)
- **Microservices**: 8080-8087 (HTTP), 9091-9097 (gRPC)

## 🎯 **Step-by-Step Execution**

### **Step 1: Prepare the Environment**

#### **1.1 Navigate to Project Root**
```bash
cd /path/to/ams-java
```

#### **1.2 Verify Project Structure**
```bash
# Check if restructured folders exist
ls -la
# Should see: backend/, frontend/, mobile/, infrastructure/, scripts/
```

#### **1.3 Make Scripts Executable (Linux/Mac)**
```bash
chmod +x scripts/test-microservices-complete.sh
chmod +x scripts/compare-endpoints.ps1
```

### **Step 2: Start Infrastructure Services**

#### **2.1 Start Database and Supporting Services**
```bash
cd infrastructure
docker-compose up -d postgres redis zipkin grafana
```

#### **2.2 Verify Infrastructure**
```bash
# Check containers are running
docker ps --filter "name=ams-"

# Test database connection
docker exec -it ams-postgres psql -U postgres -d attendance_db -c "SELECT version();"
```

#### **2.3 Wait for Services to Initialize**
```bash
# Wait 30-60 seconds for services to fully start
sleep 60
```

### **Step 3: Start Microservices**

#### **3.1 Build and Start All Microservices**
```bash
cd infrastructure
docker-compose -f docker-compose.microservices.yml up -d --build
```

#### **3.2 Monitor Startup Progress**
```bash
# Watch logs for all services
docker-compose -f docker-compose.microservices.yml logs -f

# Or check individual service logs
docker logs ams-api-gateway
docker logs ams-auth-service
docker logs ams-organization-service
# ... etc
```

#### **3.3 Wait for All Services to Start**
```bash
# Wait 2-3 minutes for all services to initialize
sleep 180
```

### **Step 4: Run Comprehensive Testing**

#### **4.1 Windows PowerShell Execution**
```powershell
# Navigate to scripts directory
cd scripts

# Run comprehensive testing
.\test-microservices-complete.ps1 -GatewayUrl "http://localhost:8080" -Verbose

# Run endpoint comparison
.\compare-endpoints.ps1 -GatewayUrl "http://localhost:8080"
```

#### **4.2 Linux/Mac Bash Execution**
```bash
# Navigate to scripts directory
cd scripts

# Run comprehensive testing
./test-microservices-complete.sh --gateway-url "http://localhost:8080" --verbose

# For endpoint comparison, use PowerShell Core if available
pwsh compare-endpoints.ps1 -GatewayUrl "http://localhost:8080"
```

### **Step 5: Manual Verification (if needed)**

#### **5.1 Test API Gateway Health**
```bash
curl -X GET http://localhost:8080/actuator/health
```

#### **5.2 Test Individual Service Health**
```bash
# Auth Service
curl -X GET http://localhost:8081/auth/actuator/health

# Organization Service  
curl -X GET http://localhost:8082/organization/actuator/health

# Menu Service
curl -X GET http://localhost:8085/menu/actuator/health

# Continue for all services...
```

#### **5.3 Test Authentication Flow**
```bash
# Super Admin Login
curl -X POST http://localhost:8080/api/auth/super/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"superadmin123"}'

# Entity Admin Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

#### **5.4 Test Public Endpoints**
```bash
# Public Menu
curl -X GET http://localhost:8080/api/menu/public/categories
curl -X GET http://localhost:8080/api/menu/public/items
```

### **Step 6: Analyze Results**

#### **6.1 Review Test Output Files**
```bash
# Check test results
cat microservices-test-results.json

# Check endpoint comparison
cat endpoint-comparison-report.json
```

#### **6.2 Check Docker Container Status**
```bash
# View all containers
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Check for any failed containers
docker ps -a --filter "status=exited"
```

#### **6.3 Review Service Logs for Errors**
```bash
# Check for errors in logs
docker logs ams-api-gateway 2>&1 | grep -i error
docker logs ams-auth-service 2>&1 | grep -i error
# Continue for all services...
```

## 🔍 **Troubleshooting Common Issues**

### **Issue 1: Services Not Starting**
```bash
# Check port conflicts
netstat -tulpn | grep :8080
netstat -tulpn | grep :5432

# Check Docker resources
docker system df
docker system prune -f
```

### **Issue 2: Database Connection Issues**
```bash
# Check PostgreSQL logs
docker logs ams-postgres

# Verify database exists
docker exec -it ams-postgres psql -U postgres -l

# Check database password
docker exec -it ams-postgres psql -U postgres -d attendance_db
```

### **Issue 3: API Gateway Routing Issues**
```bash
# Check gateway configuration
docker exec -it ams-api-gateway cat /app/application.yml

# Test direct service access
curl -X GET http://localhost:8081/auth/actuator/health
curl -X GET http://localhost:8082/organization/actuator/health
```

### **Issue 4: Authentication Failures**
```bash
# Check if default users exist in database
docker exec -it ams-postgres psql -U postgres -d attendance_db -c "SELECT * FROM super_admin;"
docker exec -it ams-postgres psql -U postgres -d attendance_db -c "SELECT * FROM entity_admin;"

# Check auth service logs
docker logs ams-auth-service | grep -i "authentication\|login\|token"
```

## 📊 **Success Criteria Checklist**

### **Infrastructure (30% weight)**
- [ ] PostgreSQL running (Port 5432)
- [ ] Redis running (Port 6379)
- [ ] Zipkin running (Port 9411)
- [ ] Grafana running (Port 3003)

### **Microservices (40% weight)**
- [ ] API Gateway running (Port 8080)
- [ ] Auth Service running (Port 8081, gRPC 9091)
- [ ] Organization Service running (Port 8082, gRPC 9092)
- [ ] Subscriber Service running (Port 8083, gRPC 9093)
- [ ] Attendance Service running (Port 8084, gRPC 9094)
- [ ] Menu Service running (Port 8085, gRPC 9095)
- [ ] Order Service running (Port 8086, gRPC 9096)
- [ ] Table Service running (Port 8087, gRPC 9097)

### **API Gateway Routing (30% weight)**
- [ ] Auth routes working (/api/auth/*)
- [ ] Organization routes working (/api/organization/*)
- [ ] Subscriber routes working (/api/subscriber/*)
- [ ] Attendance routes working (/api/attendance/*)
- [ ] Menu routes working (/api/menu/*)
- [ ] Order routes working (/api/order/*)
- [ ] Table routes working (/api/table/*)

### **Critical Endpoints (Bonus)**
- [ ] Super Admin login working
- [ ] Entity Admin login working
- [ ] Public menu access working
- [ ] Organization management working
- [ ] Basic attendance functionality working

## 🎯 **80% Completion Criteria**

**To achieve 80% microservices transition completion:**

1. **✅ All 8 microservices running** (100% uptime)
2. **✅ API Gateway routing** (≥90% routes working)
3. **✅ Critical endpoints migrated** (≥80% of critical functionality)
4. **✅ Authentication flows working** (Super Admin + Entity Admin)
5. **✅ Database connectivity** (All services connected)
6. **✅ gRPC communication** (Inter-service communication working)

**Success Threshold:**
- Overall completion ≥ 80%
- Critical endpoints ≥ 90%
- No critical services down
- Authentication working

## 📝 **Next Steps After Testing**

### **If 80%+ Complete:**
1. 🎉 **Celebrate the milestone!**
2. 📋 **Document remaining 20% tasks**
3. 🔧 **Plan performance optimization**
4. 🛡️ **Implement security hardening**
5. 📊 **Set up monitoring and alerting**

### **If <80% Complete:**
1. 🔍 **Analyze failed tests**
2. 🛠️ **Fix critical issues first**
3. 📝 **Update missing endpoints**
4. 🔄 **Re-run tests**
5. 📈 **Track progress toward 80%**

## 🆘 **Getting Help**

If you encounter issues:
1. **Check the logs** - Most issues are visible in Docker logs
2. **Verify prerequisites** - Ensure all required software is installed
3. **Check port conflicts** - Make sure required ports are available
4. **Review configuration** - Verify application.yml files in each service
5. **Test step by step** - Isolate the failing component

**Remember:** The goal is to achieve 80% completion, not 100%. Focus on critical functionality first!
