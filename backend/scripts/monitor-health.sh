#!/bin/bash

# ========== Health Check Monitor Script ==========
# Kiểm tra health của ứng dụng và gửi cảnh báo khi DOWN
# Crontab: */5 * * * * /path/to/monitor-health.sh >> /var/log/monitor.log 2>&1

# Cấu hình
HEALTH_URL="http://localhost:8088/actuator/health"
CONTAINER_NAME="spring-backend"
LOG_FILE="./logs/monitor.log"
MAX_RETRIES=3
RETRY_INTERVAL=10 # giây

# Tạo thư mục logs nếu chưa tồn tại
mkdir -p "$(dirname "$LOG_FILE")"

# Function: Kiểm tra health endpoint
check_health() {
    local retry=0
    
    while [ $retry -lt $MAX_RETRIES ]; do
        # Gọi health endpoint
        response=$(curl -s -w "\n%{http_code}" "$HEALTH_URL" 2>&1)
        http_code=$(echo "$response" | tail -n1)
        body=$(echo "$response" | sed '$d')
        
        # Kiểm tra HTTP status code
        if [ "$http_code" == "200" ]; then
            # Parse JSON để lấy status
            status=$(echo "$body" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
            
            if [ "$status" == "UP" ]; then
                echo "[$(date +'%Y-%m-%d %H:%M:%S')] ✓ Service is healthy (HTTP $http_code, Status: $status)"
                return 0
            else
                echo "[$(date +'%Y-%m-%d %H:%M:%S')] ⚠ Service status: $status (HTTP $http_code)"
                return 1
            fi
        else
            retry=$((retry + 1))
            if [ $retry -lt $MAX_RETRIES ]; then
                echo "[$(date +'%Y-%m-%d %H:%M:%S')] ⚠ Health check failed (HTTP $http_code), retrying $retry/$MAX_RETRIES..."
                sleep $RETRY_INTERVAL
            else
                echo "[$(date +'%Y-%m-%d %H:%M:%S')] ✗ Service is DOWN (HTTP $http_code after $MAX_RETRIES retries)"
                return 2
            fi
        fi
    done
}

# Function: Restart container
restart_service() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] Attempting to restart container: $CONTAINER_NAME"
    
    if docker restart "$CONTAINER_NAME" 2>&1; then
        echo "[$(date +'%Y-%m-%d %H:%M:%S')] ✓ Container restarted successfully"
        
        # Đợi 30s để service khởi động
        echo "[$(date +'%Y-%m-%d %H:%M:%S')] Waiting 30s for service to start..."
        sleep 30
        
        # Kiểm tra lại health
        if check_health; then
            echo "[$(date +'%Y-%m-%d %H:%M:%S')] ✓ Service recovered after restart"
            return 0
        else
            echo "[$(date +'%Y-%m-%d %H:%M:%S')] ✗ Service still DOWN after restart"
            return 1
        fi
    else
        echo "[$(date +'%Y-%m-%d %H:%M:%S')] ✗ Failed to restart container"
        return 1
    fi
}

# Function: Gửi notification (có thể mở rộng)
send_notification() {
    local message="$1"
    
    # TODO: Có thể gửi email, Slack, Telegram...
    # Ví dụ gửi email:
    # echo "$message" | mail -s "Server Alert" admin@example.com
    
    # Hiện tại chỉ log
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] NOTIFICATION: $message"
}

# Main logic
echo "=========================================="
echo "[$(date +'%Y-%m-%d %H:%M:%S')] Starting health check monitor"
echo "=========================================="

# Kiểm tra container có đang chạy không
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] ✗ Container $CONTAINER_NAME is not running!"
    send_notification "Container $CONTAINER_NAME is not running. Starting container..."
    
    docker start "$CONTAINER_NAME"
    sleep 30
fi

# Kiểm tra health
check_health
health_status=$?

if [ $health_status -eq 0 ]; then
    # Service healthy, không làm gì
    exit 0
elif [ $health_status -eq 1 ]; then
    # Service status không phải UP (WARNING, DOWN, etc.)
    send_notification "Service health status is not UP. Check logs for details."
    exit 1
else
    # Service DOWN, thử restart
    send_notification "Service is DOWN. Attempting automatic restart..."
    
    if restart_service; then
        send_notification "Service recovered successfully after restart."
        exit 0
    else
        send_notification "CRITICAL: Service failed to recover. Manual intervention required!"
        exit 2
    fi
fi
