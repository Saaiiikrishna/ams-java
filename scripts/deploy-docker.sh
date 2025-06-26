#!/bin/bash

# Attendance Management System - Docker Deployment Script
# This script handles building and deploying all services in Docker containers

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ENVIRONMENT=${1:-dev}  # Default to development
COMPOSE_FILE=""

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if Docker is installed and running
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    
    # Check if Docker Compose is available
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not available. Please install Docker Compose."
        exit 1
    fi
    
    print_success "Prerequisites check passed"
}

# Function to set environment
set_environment() {
    print_status "Setting up environment: $ENVIRONMENT"
    
    case $ENVIRONMENT in
        "dev"|"development")
            COMPOSE_FILE="docker-compose.yml"
            ENV_FILE=".env.dev"
            ;;
        "prod"|"production")
            COMPOSE_FILE="docker-compose.prod.yml"
            ENV_FILE=".env.prod"
            ;;
        *)
            print_error "Invalid environment: $ENVIRONMENT. Use 'dev' or 'prod'"
            exit 1
            ;;
    esac
    
    # Copy environment file if .env doesn't exist
    if [ ! -f "$PROJECT_ROOT/.env" ]; then
        if [ -f "$PROJECT_ROOT/$ENV_FILE" ]; then
            print_status "Copying $ENV_FILE to .env"
            cp "$PROJECT_ROOT/$ENV_FILE" "$PROJECT_ROOT/.env"
        else
            print_warning "Environment file $ENV_FILE not found. Using defaults."
        fi
    fi
    
    print_success "Environment set to $ENVIRONMENT"
}

# Function to build images
build_images() {
    print_status "Building Docker images..."
    
    cd "$PROJECT_ROOT"
    
    # Build backend
    print_status "Building backend image..."
    docker build -t ams-backend:latest .
    
    # Build frontend applications
    print_status "Building admin panel image..."
    docker build -t ams-admin-panel:latest ./admin-panel
    
    print_status "Building entity dashboard image..."
    docker build -t ams-entity-dashboard:latest ./entity-dashboard
    
    print_status "Building public menu image..."
    docker build -t ams-public-menu:latest ./public-menu
    
    print_success "All images built successfully"
}

# Function to start services
start_services() {
    print_status "Starting services with $COMPOSE_FILE..."
    
    cd "$PROJECT_ROOT"
    
    # Use docker-compose or docker compose based on availability
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    else
        DOCKER_COMPOSE_CMD="docker compose"
    fi
    
    # Start services
    $DOCKER_COMPOSE_CMD -f "$COMPOSE_FILE" up -d
    
    print_success "Services started successfully"
}

# Function to check service health
check_health() {
    print_status "Checking service health..."
    
    cd "$PROJECT_ROOT"
    
    # Wait for services to be healthy
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        print_status "Health check attempt $attempt/$max_attempts"
        
        # Check if all services are healthy
        if docker-compose -f "$COMPOSE_FILE" ps | grep -q "unhealthy"; then
            print_warning "Some services are still starting... (attempt $attempt/$max_attempts)"
            sleep 10
            ((attempt++))
        else
            print_success "All services are healthy"
            return 0
        fi
    done
    
    print_error "Health check failed after $max_attempts attempts"
    return 1
}

# Function to show service status
show_status() {
    print_status "Service Status:"
    echo ""
    
    cd "$PROJECT_ROOT"
    
    if command -v docker-compose &> /dev/null; then
        docker-compose -f "$COMPOSE_FILE" ps
    else
        docker compose -f "$COMPOSE_FILE" ps
    fi
    
    echo ""
    print_status "Service URLs:"
    echo "  Backend API: http://localhost:8080"
    echo "  Admin Panel: http://localhost:3001"
    echo "  Entity Dashboard: http://localhost:3002"
    echo "  Public Menu: http://localhost:3003"
    echo "  Grafana: http://localhost:3000 (admin/admin123)"
    echo "  Prometheus: http://localhost:9090"
    echo "  Zipkin: http://localhost:9411"
}

# Function to stop services
stop_services() {
    print_status "Stopping services..."
    
    cd "$PROJECT_ROOT"
    
    if command -v docker-compose &> /dev/null; then
        docker-compose -f "$COMPOSE_FILE" down
    else
        docker compose -f "$COMPOSE_FILE" down
    fi
    
    print_success "Services stopped"
}

# Function to clean up
cleanup() {
    print_status "Cleaning up Docker resources..."
    
    # Remove unused images
    docker image prune -f
    
    # Remove unused volumes (be careful with this in production)
    if [ "$ENVIRONMENT" = "dev" ] || [ "$ENVIRONMENT" = "development" ]; then
        docker volume prune -f
    fi
    
    print_success "Cleanup completed"
}

# Main execution
main() {
    print_status "Starting Attendance Management System Docker Deployment"
    print_status "Environment: $ENVIRONMENT"
    print_status "Compose file: $COMPOSE_FILE"
    echo ""
    
    check_prerequisites
    set_environment
    
    case "${2:-deploy}" in
        "build")
            build_images
            ;;
        "start")
            start_services
            check_health
            show_status
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            stop_services
            start_services
            check_health
            show_status
            ;;
        "status")
            show_status
            ;;
        "cleanup")
            cleanup
            ;;
        "deploy")
            build_images
            start_services
            check_health
            show_status
            ;;
        *)
            echo "Usage: $0 [dev|prod] [build|start|stop|restart|status|cleanup|deploy]"
            echo ""
            echo "Commands:"
            echo "  build    - Build all Docker images"
            echo "  start    - Start all services"
            echo "  stop     - Stop all services"
            echo "  restart  - Restart all services"
            echo "  status   - Show service status"
            echo "  cleanup  - Clean up Docker resources"
            echo "  deploy   - Build and start all services (default)"
            exit 1
            ;;
    esac
    
    print_success "Operation completed successfully"
}

# Run main function
main "$@"
