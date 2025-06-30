# Individual Microservice Runner
# Allows running any microservice independently with proper setup

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("auth-service", "organization-service", "subscriber-service", "attendance-service", "menu-service", "order-service", "table-service", "api-gateway")]
    [string]$ServiceName,
    
    [ValidateSet("dev", "docker", "prod")]
    [string]$Profile = "dev",
    
    [switch]$WithDatabase,
    [switch]$BuildFirst,
    [switch]$Debug,
    [string]$Port,
    [string]$GrpcPort
)

# Color functions
function Write-Success { param($Message) Write-Host $Message -ForegroundColor Green }
function Write-Error { param($Message) Write-Host $Message -ForegroundColor Red }
function Write-Info { param($Message) Write-Host $Message -ForegroundColor Cyan }
function Write-Warning { param($Message) Write-Host $Message -ForegroundColor Yellow }

# Service configuration
$ServiceConfig = @{
    "auth-service" = @{
        Path = "backend/microservices/auth-service"
        Port = 8081
        GrpcPort = 9091
        Description = "Authentication and Authorization Service"
    }
    "organization-service" = @{
        Path = "backend/microservices/organization-service"
        Port = 8082
        GrpcPort = 9092
        Description = "Organization Management Service"
    }
    "subscriber-service" = @{
        Path = "backend/microservices/subscriber-service"
        Port = 8083
        GrpcPort = 9093
        Description = "Subscriber Management Service"
    }
    "attendance-service" = @{
        Path = "backend/microservices/attendance-service"
        Port = 8084
        GrpcPort = 9094
        Description = "Attendance Tracking Service"
    }
    "menu-service" = @{
        Path = "backend/microservices/menu-service"
        Port = 8085
        GrpcPort = 9095
        Description = "Menu Management Service"
    }
    "order-service" = @{
        Path = "backend/microservices/order-service"
        Port = 8086
        GrpcPort = 9096
        Description = "Order Processing Service"
    }
    "table-service" = @{
        Path = "backend/microservices/table-service"
        Port = 8087
        GrpcPort = 9097
        Description = "Table Management Service"
    }
    "api-gateway" = @{
        Path = "backend/microservices/api-gateway"
        Port = 8080
        GrpcPort = $null
        Description = "API Gateway Service"
    }
}

$config = $ServiceConfig[$ServiceName]
if (-not $config) {
    Write-Error "❌ Unknown service: $ServiceName"
    exit 1
}

Write-Info "🚀 Starting $ServiceName"
Write-Info "📝 Description: $($config.Description)"
Write-Info "🔧 Profile: $Profile"

# Override ports if specified
if ($Port) { $config.Port = $Port }
if ($GrpcPort) { $config.GrpcPort = $GrpcPort }

Write-Info "🌐 HTTP Port: $($config.Port)"
if ($config.GrpcPort) {
    Write-Info "🔗 gRPC Port: $($config.GrpcPort)"
}

# Check if we need to start database
if ($WithDatabase) {
    Write-Info "🗄️ Starting PostgreSQL database..."
    try {
        $dbStatus = docker ps --filter "name=ams-postgres" --format "{{.Status}}"
        if (-not $dbStatus) {
            Write-Info "Starting PostgreSQL container..."
            docker-compose -f infrastructure/docker-compose.microservices.yml up -d postgres
            Write-Info "Waiting for database to be ready..."
            Start-Sleep -Seconds 10
        } else {
            Write-Success "✅ Database already running"
        }
    } catch {
        Write-Error "❌ Failed to start database: $($_.Exception.Message)"
        exit 1
    }
}

# Navigate to service directory
$servicePath = Join-Path $PSScriptRoot ".." $config.Path
if (-not (Test-Path $servicePath)) {
    Write-Error "❌ Service path not found: $servicePath"
    exit 1
}

Set-Location $servicePath

# Build shared library first if needed
if ($BuildFirst) {
    Write-Info "🔨 Building shared library..."
    try {
        Set-Location "../shared-lib"
        mvn clean install -DskipTests -q
        Write-Success "✅ Shared library built successfully"
        Set-Location "../$ServiceName"
    } catch {
        Write-Error "❌ Failed to build shared library: $($_.Exception.Message)"
        exit 1
    }
}

# Build the service if requested
if ($BuildFirst) {
    Write-Info "🔨 Building $ServiceName..."
    try {
        mvn clean package -DskipTests -q
        Write-Success "✅ $ServiceName built successfully"
    } catch {
        Write-Error "❌ Failed to build $ServiceName: $($_.Exception.Message)"
        exit 1
    }
}

# Set environment variables
$env:SPRING_PROFILES_ACTIVE = $Profile
$env:SERVER_PORT = $config.Port
if ($config.GrpcPort) {
    $env:GRPC_SERVER_PORT = $config.GrpcPort
}

# Database configuration
if ($Profile -eq "dev" -or $WithDatabase) {
    $env:JDBC_DATABASE_URL = "jdbc:postgresql://localhost:5432/attendance_db"
    $env:JDBC_DATABASE_USERNAME = "postgres"
    $env:JDBC_DATABASE_PASSWORD = "0000"
}

# Debug configuration
if ($Debug) {
    $env:JAVA_OPTS = "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005"
    Write-Info "🐛 Debug mode enabled on port 5005"
}

Write-Info "🎯 Environment Variables:"
Write-Info "   SPRING_PROFILES_ACTIVE: $env:SPRING_PROFILES_ACTIVE"
Write-Info "   SERVER_PORT: $env:SERVER_PORT"
if ($config.GrpcPort) {
    Write-Info "   GRPC_SERVER_PORT: $env:GRPC_SERVER_PORT"
}

# Start the service
Write-Success "🚀 Starting $ServiceName..."
Write-Info "📍 Service will be available at: http://localhost:$($config.Port)"
if ($config.GrpcPort) {
    Write-Info "🔗 gRPC will be available at: localhost:$($config.GrpcPort)"
}
Write-Info "⏹️ Press Ctrl+C to stop the service"
Write-Info ""

try {
    if ($BuildFirst) {
        # Run from JAR
        $jarFile = Get-ChildItem -Path "target" -Filter "*.jar" | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1
        if ($jarFile) {
            Write-Info "🏃 Running from JAR: $($jarFile.Name)"
            java -jar "target/$($jarFile.Name)"
        } else {
            Write-Error "❌ No JAR file found in target directory"
            exit 1
        }
    } else {
        # Run with Maven
        Write-Info "🏃 Running with Maven Spring Boot plugin..."
        mvn spring-boot:run
    }
} catch {
    Write-Error "❌ Failed to start $ServiceName: $($_.Exception.Message)"
    exit 1
}

Write-Info "👋 $ServiceName stopped"
