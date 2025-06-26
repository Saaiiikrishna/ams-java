#!/bin/bash

# Attendance Management System Deployment Script
# Comprehensive deployment with health checks and rollback capabilities

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yml"
ENV_FILE="$PROJECT_ROOT/.env"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose is not installed or not in PATH"
        exit 1
    fi
    
    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Create environment file if it doesn't exist
create_env_file() {
    if [[ ! -f "$ENV_FILE" ]]; then
        log_info "Creating environment file..."
        cat > "$ENV_FILE" << EOF
# Attendance Management System Environment Configuration

# Database
POSTGRES_PASSWORD=attendance_secure_$(date +%Y)

# Security
JWT_SECRET=ams_production_secret_key_$(date +%Y)_very_secure

# Monitoring
GRAFANA_PASSWORD=admin123

# Application
ENVIRONMENT=docker
REGION=local
EOF
        log_success "Environment file created at $ENV_FILE"
    else
        log_info "Environment file already exists"
    fi
}

# Build images
build_images() {
    log_info "Building Docker images..."
    
    cd "$PROJECT_ROOT"
    
    # Build backend
    log_info "Building backend image..."
    docker build -t ams-backend:latest .
    
    # Build admin panel
    if [[ -d "admin-panel" ]]; then
        log_info "Building admin panel image..."
        docker build -t ams-admin-panel:latest ./admin-panel
    fi
    
    # Build entity dashboard
    if [[ -d "entity-dashboard" ]]; then
        log_info "Building entity dashboard image..."
        docker build -t ams-entity-dashboard:latest ./entity-dashboard
    fi
    
    log_success "All images built successfully"
}

# Deploy services
deploy_services() {
    log_info "Deploying services..."
    
    cd "$PROJECT_ROOT"
    
    # Create necessary directories
    mkdir -p docker/prometheus docker/grafana/provisioning docker/grafana/dashboards
    
    # Start services
    docker-compose -f "$COMPOSE_FILE" up -d
    
    log_success "Services deployment initiated"
}

# Wait for services to be healthy
wait_for_services() {
    log_info "Waiting for services to be healthy..."
    
    local max_attempts=60
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        log_info "Health check attempt $attempt/$max_attempts"
        
        # Check database
        if docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U postgres -d attendance_db &> /dev/null; then
            log_success "Database is healthy"
        else
            log_warning "Database not ready yet..."
        fi
        
        # Check backend
        if curl -f http://localhost:8080/actuator/health &> /dev/null; then
            log_success "Backend is healthy"
        else
            log_warning "Backend not ready yet..."
        fi
        
        # Check if all services are healthy
        local unhealthy_services
        unhealthy_services=$(docker-compose -f "$COMPOSE_FILE" ps --filter "health=unhealthy" -q)
        
        if [[ -z "$unhealthy_services" ]]; then
            log_success "All services are healthy!"
            return 0
        fi
        
        sleep 10
        ((attempt++))
    done
    
    log_error "Services did not become healthy within the timeout period"
    return 1
}

# Show service status
show_status() {
    log_info "Service Status:"
    docker-compose -f "$COMPOSE_FILE" ps
    
    echo ""
    log_info "Service URLs:"
    echo "  🌐 Admin Panel: http://localhost:3001"
    echo "  🏢 Entity Dashboard: http://localhost:3002"
    echo "  🔧 Backend API: http://localhost:8080"
    echo "  📊 Grafana: http://localhost:3000 (admin/admin123)"
    echo "  📈 Prometheus: http://localhost:9090"
    echo "  🔍 Zipkin: http://localhost:9411"
    echo "  🗄️ Database: localhost:5432"
}

# Rollback function
rollback() {
    log_warning "Rolling back deployment..."
    
    cd "$PROJECT_ROOT"
    docker-compose -f "$COMPOSE_FILE" down
    
    log_success "Rollback completed"
}

# Cleanup function
cleanup() {
    log_info "Cleaning up..."
    
    cd "$PROJECT_ROOT"
    
    # Stop and remove containers
    docker-compose -f "$COMPOSE_FILE" down -v
    
    # Remove images (optional)
    if [[ "${1:-}" == "--remove-images" ]]; then
        log_info "Removing images..."
        docker rmi ams-backend:latest ams-admin-panel:latest ams-entity-dashboard:latest 2>/dev/null || true
    fi
    
    log_success "Cleanup completed"
}

# Main deployment function
main() {
    local command="${1:-deploy}"
    
    case "$command" in
        "deploy")
            log_info "Starting Attendance Management System deployment..."
            check_prerequisites
            create_env_file
            build_images
            deploy_services
            
            if wait_for_services; then
                show_status
                log_success "🎉 Deployment completed successfully!"
            else
                log_error "Deployment failed - services are not healthy"
                rollback
                exit 1
            fi
            ;;
        "status")
            show_status
            ;;
        "rollback")
            rollback
            ;;
        "cleanup")
            cleanup "${2:-}"
            ;;
        "logs")
            docker-compose -f "$COMPOSE_FILE" logs -f "${2:-}"
            ;;
        *)
            echo "Usage: $0 {deploy|status|rollback|cleanup|logs}"
            echo ""
            echo "Commands:"
            echo "  deploy   - Deploy the entire system"
            echo "  status   - Show service status and URLs"
            echo "  rollback - Rollback the deployment"
            echo "  cleanup  - Stop and remove all containers"
            echo "  logs     - Show logs (optionally for specific service)"
            exit 1
            ;;
    esac
}

# Handle script interruption
trap 'log_error "Script interrupted"; exit 1' INT TERM

# Run main function
main "$@"
