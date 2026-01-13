#!/bin/bash

# ========== Full Stack Deploy Script ==========
# Script deploy cả Backend + Frontend

set -e # Exit on error

COMPOSE_FILE="docker-compose.yml"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_step() {
    echo -e "${BLUE}==>${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

echo ""
echo "=========================================="
echo "  Đấu Trường Tri Thức - Full Stack Deploy"
echo "=========================================="
echo ""

# 1. Check prerequisites
print_step "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed!"
    exit 1
fi
print_success "Docker installed"

if ! docker compose version &> /dev/null; then
    print_error "Docker Compose is not installed!"
    exit 1
fi
print_success "Docker Compose installed"

# 2. Check environment
print_step "Checking environment configuration..."

if [ ! -f "../frontend/src/app/environments/environment.prod.ts" ]; then
    print_error "Frontend environment.prod.ts not found!"
    exit 1
fi

if grep -q "your-domain.com" "../frontend/src/app/environments/environment.prod.ts"; then
    print_warning "⚠️  Remember to update domain in frontend/src/app/environments/environment.prod.ts"
    read -p "Continue anyway? (y/n): " confirm
    if [ "$confirm" != "y" ]; then
        exit 0
    fi
fi
print_success "Environment files exist"

# 3. Stop existing containers
print_step "Stopping existing containers..."
docker compose -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true
print_success "Old containers stopped"

# 4. Build images
print_step "Building Docker images (this may take a few minutes)..."
docker compose -f "$COMPOSE_FILE" build --no-cache

if [ $? -eq 0 ]; then
    print_success "Images built successfully"
else
    print_error "Failed to build images"
    exit 1
fi

# 5. Start services
print_step "Starting all services..."
docker compose -f "$COMPOSE_FILE" up -d

if [ $? -eq 0 ]; then
    print_success "Services started"
else
    print_error "Failed to start services"
    exit 1
fi

# 6. Wait for services to be healthy
print_step "Waiting for services to be healthy..."
sleep 30

# Check MySQL
if docker exec mysql-dautruong mysqladmin ping -h localhost -u root -pdongle170503 &> /dev/null; then
    print_success "MySQL is healthy"
else
    print_warning "MySQL may still be starting..."
fi

# Check Redis
if docker exec redis-dautruong redis-cli ping | grep -q "PONG"; then
    print_success "Redis is healthy"
else
    print_warning "Redis may still be starting..."
fi

# Check Spring Boot
if curl -s http://localhost:8088/actuator/health | grep -q "UP"; then
    print_success "Spring Boot is healthy"
else
    print_warning "Spring Boot may still be starting (wait 30-60s more)"
fi

# Check Frontend
if curl -s http://localhost/health | grep -q "healthy"; then
    print_success "Frontend (Nginx) is healthy"
else
    print_warning "Frontend may still be starting..."
fi

# 7. Show status
echo ""
echo "=========================================="
echo "  Deploy Complete!"
echo "=========================================="
echo ""
docker compose -f "$COMPOSE_FILE" ps
echo ""
echo "📍 Access URLs:"
echo "   Frontend:  http://localhost"
echo "   Backend:   http://localhost:8088"
echo "   API Docs:  http://localhost:8088/api/v1"
echo "   Health:    http://localhost:8088/actuator/health"
echo ""
echo "📊 Resource Usage:"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"
echo ""
echo "📝 View logs:"
echo "   bash scripts/manage-service.sh logs"
echo ""
