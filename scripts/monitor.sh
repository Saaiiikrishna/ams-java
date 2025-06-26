#!/bin/bash

# Container Health Monitoring Script for Attendance Management System
# Provides comprehensive monitoring and alerting for containerized services

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yml"
LOG_FILE="$PROJECT_ROOT/logs/monitor.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    local message="[$(date '+%Y-%m-%d %H:%M:%S')] [INFO] $1"
    echo -e "${BLUE}$message${NC}"
    echo "$message" >> "$LOG_FILE"
}

log_success() {
    local message="[$(date '+%Y-%m-%d %H:%M:%S')] [SUCCESS] $1"
    echo -e "${GREEN}$message${NC}"
    echo "$message" >> "$LOG_FILE"
}

log_warning() {
    local message="[$(date '+%Y-%m-%d %H:%M:%S')] [WARNING] $1"
    echo -e "${YELLOW}$message${NC}"
    echo "$message" >> "$LOG_FILE"
}

log_error() {
    local message="[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] $1"
    echo -e "${RED}$message${NC}"
    echo "$message" >> "$LOG_FILE"
}

# Create log directory
mkdir -p "$(dirname "$LOG_FILE")"

# Check container health
check_container_health() {
    local container_name="$1"
    local health_status
    
    if docker ps --filter "name=$container_name" --filter "status=running" | grep -q "$container_name"; then
        health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "no-healthcheck")
        
        case "$health_status" in
            "healthy")
                log_success "Container $container_name is healthy"
                return 0
                ;;
            "unhealthy")
                log_error "Container $container_name is unhealthy"
                return 1
                ;;
            "starting")
                log_warning "Container $container_name is starting"
                return 2
                ;;
            "no-healthcheck")
                log_warning "Container $container_name has no health check configured"
                return 2
                ;;
            *)
                log_warning "Container $container_name has unknown health status: $health_status"
                return 2
                ;;
        esac
    else
        log_error "Container $container_name is not running"
        return 1
    fi
}

# Check service endpoint
check_service_endpoint() {
    local service_name="$1"
    local endpoint="$2"
    local expected_status="${3:-200}"
    
    local response_code
    response_code=$(curl -s -o /dev/null -w "%{http_code}" "$endpoint" || echo "000")
    
    if [[ "$response_code" == "$expected_status" ]]; then
        log_success "Service $service_name endpoint $endpoint is responding correctly ($response_code)"
        return 0
    else
        log_error "Service $service_name endpoint $endpoint returned $response_code (expected $expected_status)"
        return 1
    fi
}

# Check resource usage
check_resource_usage() {
    local container_name="$1"
    local memory_limit_mb="${2:-1024}"
    local cpu_limit_percent="${3:-80}"
    
    if ! docker ps --filter "name=$container_name" --filter "status=running" | grep -q "$container_name"; then
        log_error "Container $container_name is not running"
        return 1
    fi
    
    # Get container stats
    local stats
    stats=$(docker stats --no-stream --format "table {{.MemUsage}}\t{{.CPUPerc}}" "$container_name" | tail -n 1)
    
    # Parse memory usage (format: "used / limit")
    local memory_used_mb
    memory_used_mb=$(echo "$stats" | awk '{print $1}' | sed 's/MiB//' | sed 's/GiB/*1024/' | bc 2>/dev/null || echo "0")
    
    # Parse CPU usage (format: "XX.XX%")
    local cpu_percent
    cpu_percent=$(echo "$stats" | awk '{print $2}' | sed 's/%//' | cut -d'.' -f1)
    
    # Check memory usage
    if (( $(echo "$memory_used_mb > $memory_limit_mb" | bc -l) )); then
        log_warning "Container $container_name memory usage is high: ${memory_used_mb}MB (limit: ${memory_limit_mb}MB)"
    else
        log_info "Container $container_name memory usage: ${memory_used_mb}MB"
    fi
    
    # Check CPU usage
    if (( cpu_percent > cpu_limit_percent )); then
        log_warning "Container $container_name CPU usage is high: ${cpu_percent}% (limit: ${cpu_limit_percent}%)"
    else
        log_info "Container $container_name CPU usage: ${cpu_percent}%"
    fi
}

# Check disk usage
check_disk_usage() {
    local volume_name="$1"
    local usage_limit_percent="${2:-80}"
    
    local usage_info
    usage_info=$(docker system df -v | grep "$volume_name" | awk '{print $3}' | head -n 1)
    
    if [[ -n "$usage_info" ]]; then
        log_info "Volume $volume_name usage: $usage_info"
    else
        log_warning "Could not get usage information for volume $volume_name"
    fi
}

# Comprehensive health check
comprehensive_health_check() {
    log_info "Starting comprehensive health check..."
    
    local overall_status=0
    
    # Check container health
    local containers=("ams-postgres" "ams-backend" "admin-panel" "entity-dashboard" "zipkin" "prometheus" "grafana")
    
    for container in "${containers[@]}"; do
        if ! check_container_health "$container"; then
            overall_status=1
        fi
    done
    
    # Check service endpoints
    log_info "Checking service endpoints..."
    
    # Backend health check
    if ! check_service_endpoint "Backend" "http://localhost:8080/actuator/health"; then
        overall_status=1
    fi
    
    # Admin panel
    if ! check_service_endpoint "Admin Panel" "http://localhost:3001/health"; then
        overall_status=1
    fi
    
    # Entity dashboard
    if ! check_service_endpoint "Entity Dashboard" "http://localhost:3002/health"; then
        overall_status=1
    fi
    
    # Prometheus
    if ! check_service_endpoint "Prometheus" "http://localhost:9090/-/healthy"; then
        overall_status=1
    fi
    
    # Grafana
    if ! check_service_endpoint "Grafana" "http://localhost:3000/api/health"; then
        overall_status=1
    fi
    
    # Zipkin
    if ! check_service_endpoint "Zipkin" "http://localhost:9411/health"; then
        overall_status=1
    fi
    
    # Check resource usage
    log_info "Checking resource usage..."
    check_resource_usage "ams-backend" 1536 80
    check_resource_usage "ams-postgres" 512 70
    check_resource_usage "prometheus" 1024 70
    check_resource_usage "grafana" 512 60
    
    # Check disk usage
    log_info "Checking disk usage..."
    check_disk_usage "postgres_data"
    check_disk_usage "prometheus_data"
    check_disk_usage "grafana_data"
    
    if [[ $overall_status -eq 0 ]]; then
        log_success "All health checks passed!"
    else
        log_error "Some health checks failed!"
    fi
    
    return $overall_status
}

# Generate health report
generate_health_report() {
    local report_file="$PROJECT_ROOT/logs/health_report_$(date +%Y%m%d_%H%M%S).json"
    
    log_info "Generating health report..."
    
    # Get container information
    local containers_info
    containers_info=$(docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | tail -n +2)
    
    # Get system information
    local system_info
    system_info=$(docker system df)
    
    # Create JSON report
    cat > "$report_file" << EOF
{
  "timestamp": "$(date -Iseconds)",
  "system_info": {
    "docker_version": "$(docker --version)",
    "compose_version": "$(docker-compose --version 2>/dev/null || echo 'N/A')"
  },
  "containers": [
EOF

    # Add container information
    local first=true
    while IFS=$'\t' read -r name status ports; do
        if [[ "$first" == true ]]; then
            first=false
        else
            echo "," >> "$report_file"
        fi
        
        local health_status
        health_status=$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null || echo "no-healthcheck")
        
        cat >> "$report_file" << EOF
    {
      "name": "$name",
      "status": "$status",
      "health": "$health_status",
      "ports": "$ports"
    }
EOF
    done <<< "$containers_info"
    
    cat >> "$report_file" << EOF
  ],
  "system_usage": $(docker system df --format json 2>/dev/null || echo '{}')
}
EOF
    
    log_success "Health report generated: $report_file"
}

# Restart unhealthy containers
restart_unhealthy_containers() {
    log_info "Checking for unhealthy containers to restart..."
    
    local unhealthy_containers
    unhealthy_containers=$(docker ps --filter "health=unhealthy" --format "{{.Names}}")
    
    if [[ -n "$unhealthy_containers" ]]; then
        log_warning "Found unhealthy containers: $unhealthy_containers"
        
        for container in $unhealthy_containers; do
            log_info "Restarting unhealthy container: $container"
            docker restart "$container"
            log_success "Restarted container: $container"
        done
    else
        log_info "No unhealthy containers found"
    fi
}

# Main monitoring function
main() {
    local command="${1:-check}"
    
    case "$command" in
        "check")
            comprehensive_health_check
            ;;
        "report")
            generate_health_report
            ;;
        "restart")
            restart_unhealthy_containers
            ;;
        "watch")
            log_info "Starting continuous monitoring (press Ctrl+C to stop)..."
            while true; do
                comprehensive_health_check
                echo ""
                sleep 60
            done
            ;;
        "logs")
            local service="${2:-}"
            if [[ -n "$service" ]]; then
                docker-compose -f "$COMPOSE_FILE" logs -f "$service"
            else
                docker-compose -f "$COMPOSE_FILE" logs -f
            fi
            ;;
        *)
            echo "Usage: $0 {check|report|restart|watch|logs [service]}"
            echo ""
            echo "Commands:"
            echo "  check   - Perform comprehensive health check"
            echo "  report  - Generate detailed health report"
            echo "  restart - Restart unhealthy containers"
            echo "  watch   - Continuous monitoring"
            echo "  logs    - Show logs (optionally for specific service)"
            exit 1
            ;;
    esac
}

# Handle script interruption
trap 'log_info "Monitoring stopped"; exit 0' INT TERM

# Run main function
main "$@"
