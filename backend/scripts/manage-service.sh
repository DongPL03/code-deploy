#!/bin/bash

# ========== Service Management Script ==========
# Script quản lý Docker services (start, stop, restart, logs, status)

COMPOSE_FILE="docker-compose.yml"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check if docker-compose is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed!"
        exit 1
    fi
    
    if ! docker compose version &> /dev/null && ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed!"
        exit 1
    fi
}

# Start services
start_services() {
    print_info "Starting all services..."
    docker compose -f "$COMPOSE_FILE" up -d
    
    if [ $? -eq 0 ]; then
        print_success "Services started successfully"
        echo ""
        print_info "Waiting for services to be healthy..."
        sleep 10
        show_status
    else
        print_error "Failed to start services"
        exit 1
    fi
}

# Stop services
stop_services() {
    print_info "Stopping all services..."
    docker compose -f "$COMPOSE_FILE" down
    
    if [ $? -eq 0 ]; then
        print_success "Services stopped successfully"
    else
        print_error "Failed to stop services"
        exit 1
    fi
}

# Restart services
restart_services() {
    print_info "Restarting all services..."
    docker compose -f "$COMPOSE_FILE" restart
    
    if [ $? -eq 0 ]; then
        print_success "Services restarted successfully"
        echo ""
        print_info "Waiting for services to be healthy..."
        sleep 10
        show_status
    else
        print_error "Failed to restart services"
        exit 1
    fi
}

# Show status
show_status() {
    echo ""
    echo "=========================================="
    echo "Service Status"
    echo "=========================================="
    docker compose -f "$COMPOSE_FILE" ps
    echo ""
    
    # Check health endpoint
    print_info "Checking application health..."
    health_response=$(curl -s http://localhost:8088/actuator/health)
    
    if [ $? -eq 0 ]; then
        status=$(echo "$health_response" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
        if [ "$status" == "UP" ]; then
            print_success "Application is healthy (Status: $status)"
        else
            print_warning "Application status: $status"
        fi
    else
        print_error "Cannot reach health endpoint"
    fi
}

# Show logs
show_logs() {
    local service=$1
    
    if [ -z "$service" ]; then
        print_info "Showing logs for all services (press Ctrl+C to exit)..."
        docker compose -f "$COMPOSE_FILE" logs -f --tail=100
    else
        print_info "Showing logs for $service (press Ctrl+C to exit)..."
        docker compose -f "$COMPOSE_FILE" logs -f --tail=100 "$service"
    fi
}

# Rebuild services
rebuild_services() {
    print_info "Rebuilding and restarting services..."
    docker compose -f "$COMPOSE_FILE" down
    docker compose -f "$COMPOSE_FILE" up -d --build
    
    if [ $? -eq 0 ]; then
        print_success "Services rebuilt and started successfully"
        echo ""
        print_info "Waiting for services to be healthy..."
        sleep 15
        show_status
    else
        print_error "Failed to rebuild services"
        exit 1
    fi
}

# Show help
show_help() {
    echo "Usage: $0 {start|stop|restart|status|logs|rebuild|help} [service-name]"
    echo ""
    echo "Commands:"
    echo "  start      - Start all services"
    echo "  stop       - Stop all services"
    echo "  restart    - Restart all services"
    echo "  status     - Show service status and health"
    echo "  logs       - Show logs (optional: specify service name)"
    echo "  rebuild    - Rebuild and restart all services"
    echo "  help       - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 start"
    echo "  $0 logs spring-app-container"
    echo "  $0 status"
}

# Main
check_docker

case "$1" in
    start)
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        restart_services
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs "$2"
        ;;
    rebuild)
        rebuild_services
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Invalid command: $1"
        echo ""
        show_help
        exit 1
        ;;
esac
