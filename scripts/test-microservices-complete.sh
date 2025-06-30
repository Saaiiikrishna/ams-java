#!/bin/bash

# Comprehensive Microservices Testing Script
# Tests all endpoints through API Gateway and individual services

GATEWAY_URL="http://localhost:8080"
SKIP_AUTH=false
OUTPUT_FILE="microservices-test-results.json"
VERBOSE=false

# Color functions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_success() { echo -e "${GREEN}$1${NC}"; }
print_error() { echo -e "${RED}$1${NC}"; }
print_info() { echo -e "${CYAN}$1${NC}"; }
print_warning() { echo -e "${YELLOW}$1${NC}"; }

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --gateway-url)
            GATEWAY_URL="$2"
            shift 2
            ;;
        --skip-auth)
            SKIP_AUTH=true
            shift
            ;;
        --output-file)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

print_info "🚀 Starting Comprehensive Microservices Testing"
print_info "Gateway URL: $GATEWAY_URL"
print_info "Output File: $OUTPUT_FILE"

# Initialize results file
cat > "$OUTPUT_FILE" << EOF
{
    "timestamp": "$(date -Iseconds)",
    "gatewayUrl": "$GATEWAY_URL",
    "infrastructure": {},
    "services": {},
    "gateway": {},
    "authentication": {},
    "endpoints": {},
    "summary": {}
}
EOF

# Function to test port connectivity
test_port() {
    local host=$1
    local port=$2
    local timeout=5
    
    if command -v nc >/dev/null 2>&1; then
        nc -z -w$timeout $host $port >/dev/null 2>&1
    elif command -v telnet >/dev/null 2>&1; then
        timeout $timeout telnet $host $port >/dev/null 2>&1
    else
        # Fallback using /dev/tcp (bash built-in)
        timeout $timeout bash -c "exec 3<>/dev/tcp/$host/$port" >/dev/null 2>&1
    fi
}

# Function to make HTTP request
make_request() {
    local method=$1
    local url=$2
    local headers=$3
    local data=$4
    
    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    
    if [[ -n "$headers" ]]; then
        curl_cmd="$curl_cmd $headers"
    fi
    
    if [[ -n "$data" ]]; then
        curl_cmd="$curl_cmd -d '$data' -H 'Content-Type: application/json'"
    fi
    
    curl_cmd="$curl_cmd '$url'"
    
    eval $curl_cmd
}

# Phase 1: Infrastructure Health Checks
print_info "\n=== Phase 1: Infrastructure Health Checks ==="

declare -A infrastructure_ports=(
    ["PostgreSQL"]=5432
    ["Redis"]=6379
    ["Zipkin"]=9411
    ["Grafana"]=3003
    ["Elasticsearch"]=9200
)

for service in "${!infrastructure_ports[@]}"; do
    port=${infrastructure_ports[$service]}
    if test_port localhost $port; then
        print_success "✅ $service (Port $port): RUNNING"
    else
        print_error "❌ $service (Port $port): NOT ACCESSIBLE"
    fi
done

# Phase 2: Microservices Health Checks
print_info "\n=== Phase 2: Microservices Health Checks ==="

declare -A microservice_ports=(
    ["API Gateway"]=8080
    ["Auth Service"]=8081
    ["Organization Service"]=8082
    ["Subscriber Service"]=8083
    ["Attendance Service"]=8084
    ["Menu Service"]=8085
    ["Order Service"]=8086
    ["Table Service"]=8087
)

for service in "${!microservice_ports[@]}"; do
    port=${microservice_ports[$service]}
    if test_port localhost $port; then
        print_success "✅ $service (Port $port): RUNNING"
    else
        print_error "❌ $service (Port $port): NOT ACCESSIBLE"
    fi
done

# Phase 3: gRPC Ports Check
print_info "\n=== Phase 3: gRPC Ports Check ==="

declare -A grpc_ports=(
    ["Auth Service gRPC"]=9091
    ["Organization Service gRPC"]=9092
    ["Subscriber Service gRPC"]=9093
    ["Attendance Service gRPC"]=9094
    ["Menu Service gRPC"]=9095
    ["Order Service gRPC"]=9096
    ["Table Service gRPC"]=9097
)

for service in "${!grpc_ports[@]}"; do
    port=${grpc_ports[$service]}
    if test_port localhost $port; then
        print_success "✅ $service (Port $port): OPEN"
    else
        print_error "❌ $service (Port $port): CLOSED"
    fi
done

# Phase 4: API Gateway Health
print_info "\n=== Phase 4: API Gateway Health ==="

gateway_health_response=$(curl -s -w '%{http_code}' "$GATEWAY_URL/actuator/health")
gateway_health_code="${gateway_health_response: -3}"
gateway_health_body="${gateway_health_response%???}"

if [[ "$gateway_health_code" == "200" ]]; then
    print_success "✅ API Gateway Health: 200 OK"
else
    print_error "❌ API Gateway Health Check Failed: $gateway_health_code"
fi

# Phase 5: Service Health Endpoints
print_info "\n=== Phase 5: Service Health Endpoints ==="

declare -A service_health_endpoints=(
    ["Auth Service"]="$GATEWAY_URL/api/auth/actuator/health"
    ["Organization Service"]="$GATEWAY_URL/api/organization/actuator/health"
    ["Subscriber Service"]="$GATEWAY_URL/api/subscriber/actuator/health"
    ["Attendance Service"]="$GATEWAY_URL/api/attendance/actuator/health"
    ["Menu Service"]="$GATEWAY_URL/api/menu/actuator/health"
    ["Order Service"]="$GATEWAY_URL/api/order/actuator/health"
    ["Table Service"]="$GATEWAY_URL/api/table/actuator/health"
)

for service in "${!service_health_endpoints[@]}"; do
    endpoint=${service_health_endpoints[$service]}
    response=$(curl -s -w '%{http_code}' "$endpoint")
    status_code="${response: -3}"
    
    if [[ "$status_code" == "200" ]]; then
        print_success "✅ $service Health: 200 OK"
    else
        print_error "❌ $service Health Failed: $status_code"
    fi
done

# Phase 6: Authentication Testing
if [[ "$SKIP_AUTH" != "true" ]]; then
    print_info "\n=== Phase 6: Authentication Testing ==="
    
    # Test Super Admin Login
    print_info "Testing Super Admin Authentication..."
    super_admin_data='{"username":"superadmin","password":"superadmin123"}'
    super_admin_response=$(curl -s -w '%{http_code}' -X POST -H "Content-Type: application/json" -d "$super_admin_data" "$GATEWAY_URL/api/auth/super/login")
    super_admin_code="${super_admin_response: -3}"
    super_admin_body="${super_admin_response%???}"
    
    if [[ "$super_admin_code" == "200" ]]; then
        print_success "✅ Super Admin Login: SUCCESS"
        super_admin_token=$(echo "$super_admin_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    else
        print_error "❌ Super Admin Login Failed: $super_admin_code"
    fi
    
    # Test Entity Admin Login
    print_info "Testing Entity Admin Authentication..."
    entity_admin_data='{"username":"admin","password":"admin123"}'
    entity_admin_response=$(curl -s -w '%{http_code}' -X POST -H "Content-Type: application/json" -d "$entity_admin_data" "$GATEWAY_URL/api/auth/login")
    entity_admin_code="${entity_admin_response: -3}"
    entity_admin_body="${entity_admin_response%???}"
    
    if [[ "$entity_admin_code" == "200" ]]; then
        print_success "✅ Entity Admin Login: SUCCESS"
        entity_admin_token=$(echo "$entity_admin_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    else
        print_error "❌ Entity Admin Login Failed: $entity_admin_code"
    fi
fi

# Phase 7: Endpoint Testing
print_info "\n=== Phase 7: Endpoint Testing ==="

# Test public endpoints
print_info "Testing Public Endpoints..."

public_endpoints=(
    "Public Menu Categories:$GATEWAY_URL/api/menu/public/categories"
    "Public Menu Items:$GATEWAY_URL/api/menu/public/items"
)

for endpoint_info in "${public_endpoints[@]}"; do
    IFS=':' read -r name url <<< "$endpoint_info"
    response=$(curl -s -w '%{http_code}' "$url")
    status_code="${response: -3}"
    
    if [[ "$status_code" == "200" ]]; then
        print_success "✅ $name: 200 OK"
    else
        print_error "❌ $name: $status_code"
    fi
done

# Test protected endpoints (if we have tokens)
if [[ "$SKIP_AUTH" != "true" && (-n "$super_admin_token" || -n "$entity_admin_token") ]]; then
    print_info "Testing Protected Endpoints..."
    
    # Super Admin endpoints
    if [[ -n "$super_admin_token" ]]; then
        super_admin_endpoints=(
            "Organization Entities:$GATEWAY_URL/api/organization/entities"
        )
        
        for endpoint_info in "${super_admin_endpoints[@]}"; do
            IFS=':' read -r name url <<< "$endpoint_info"
            response=$(curl -s -w '%{http_code}' -H "Authorization: Bearer $super_admin_token" "$url")
            status_code="${response: -3}"
            
            if [[ "$status_code" == "200" ]]; then
                print_success "✅ $name: 200 OK"
            else
                print_error "❌ $name: $status_code"
            fi
        done
    fi
    
    # Entity Admin endpoints
    if [[ -n "$entity_admin_token" ]]; then
        entity_admin_endpoints=(
            "Menu Categories:$GATEWAY_URL/api/menu/categories"
            "Menu Items:$GATEWAY_URL/api/menu/items"
            "Attendance Sessions:$GATEWAY_URL/api/attendance/sessions"
            "Tables List:$GATEWAY_URL/api/table/list"
        )
        
        for endpoint_info in "${entity_admin_endpoints[@]}"; do
            IFS=':' read -r name url <<< "$endpoint_info"
            response=$(curl -s -w '%{http_code}' -H "Authorization: Bearer $entity_admin_token" "$url")
            status_code="${response: -3}"
            
            if [[ "$status_code" == "200" ]]; then
                print_success "✅ $name: 200 OK"
            else
                print_error "❌ $name: $status_code"
            fi
        done
    fi
fi

# Phase 8: Generate Summary
print_info "\n=== Phase 8: Test Summary ==="

print_info "\n🎯 MICROSERVICES TESTING RESULTS"
print_info "================================"

# Count running services
running_infrastructure=0
total_infrastructure=${#infrastructure_ports[@]}
for service in "${!infrastructure_ports[@]}"; do
    port=${infrastructure_ports[$service]}
    if test_port localhost $port; then
        ((running_infrastructure++))
    fi
done

running_services=0
total_services=${#microservice_ports[@]}
for service in "${!microservice_ports[@]}"; do
    port=${microservice_ports[$service]}
    if test_port localhost $port; then
        ((running_services++))
    fi
done

running_grpc=0
total_grpc=${#grpc_ports[@]}
for service in "${!grpc_ports[@]}"; do
    port=${grpc_ports[$service]}
    if test_port localhost $port; then
        ((running_grpc++))
    fi
done

infrastructure_percentage=$((running_infrastructure * 100 / total_infrastructure))
services_percentage=$((running_services * 100 / total_services))
grpc_percentage=$((running_grpc * 100 / total_grpc))

overall_completion=$(((infrastructure_percentage * 30 + services_percentage * 40 + grpc_percentage * 30) / 100))

print_info "Infrastructure: $running_infrastructure/$total_infrastructure ($infrastructure_percentage%)"
print_info "Services: $running_services/$total_services ($services_percentage%)"
print_info "gRPC Ports: $running_grpc/$total_grpc ($grpc_percentage%)"
print_info ""

if [[ $overall_completion -ge 80 ]]; then
    print_success "🎉 OVERALL COMPLETION: $overall_completion% - MICROSERVICES TRANSITION 80%+ COMPLETE!"
elif [[ $overall_completion -ge 60 ]]; then
    print_warning "⚠️ OVERALL COMPLETION: $overall_completion% - Good progress, needs some fixes"
else
    print_error "❌ OVERALL COMPLETION: $overall_completion% - Significant issues need resolution"
fi

print_info "\n🚀 Testing Complete!"
print_info "Next steps:"
print_info "1. Review failed services and endpoints"
print_info "2. Check Docker container logs: docker logs <container-name>"
print_info "3. Verify database connectivity and migrations"
print_info "4. Test authentication flows manually if needed"

# Update results file with summary
cat > "$OUTPUT_FILE" << EOF
{
    "timestamp": "$(date -Iseconds)",
    "gatewayUrl": "$GATEWAY_URL",
    "summary": {
        "infrastructure": {
            "running": $running_infrastructure,
            "total": $total_infrastructure,
            "percentage": $infrastructure_percentage
        },
        "services": {
            "running": $running_services,
            "total": $total_services,
            "percentage": $services_percentage
        },
        "grpc": {
            "running": $running_grpc,
            "total": $total_grpc,
            "percentage": $grpc_percentage
        },
        "overallCompletion": $overall_completion
    }
}
EOF

print_success "\n✅ Test results saved to: $OUTPUT_FILE"
