#!/bin/bash

# ========== MySQL Backup Script cho Docker ==========
# Tự động backup database và giữ 7 ngày gần nhất
# Crontab: 0 2 * * * /path/to/backup-database.sh >> /var/log/backup.log 2>&1

# Cấu hình
CONTAINER_NAME="mysql-dautruong"
DB_NAME="dau_truong_tri_thuc"
DB_USER="root"
DB_PASSWORD="dongle170503"
BACKUP_DIR="./backups/mysql"
RETENTION_DAYS=7

# Tạo thư mục backup nếu chưa tồn tại
mkdir -p "$BACKUP_DIR"

# Tên file backup với timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_${TIMESTAMP}.sql.gz"

echo "=========================================="
echo "Starting MySQL Backup: $(date)"
echo "=========================================="

# Kiểm tra container có đang chạy không
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "ERROR: Container $CONTAINER_NAME is not running!"
    exit 1
fi

# Thực hiện backup và nén
echo "Backing up database: $DB_NAME"
docker exec "$CONTAINER_NAME" mysqldump \
    -u"$DB_USER" \
    -p"$DB_PASSWORD" \
    --single-transaction \
    --quick \
    --lock-tables=false \
    --routines \
    --triggers \
    --events \
    "$DB_NAME" | gzip > "$BACKUP_FILE"

# Kiểm tra backup có thành công không
if [ $? -eq 0 ]; then
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "✓ Backup successful: $BACKUP_FILE ($BACKUP_SIZE)"
else
    echo "✗ Backup failed!"
    exit 1
fi

# Xóa các backup cũ hơn RETENTION_DAYS ngày
echo "Cleaning old backups (older than $RETENTION_DAYS days)..."
find "$BACKUP_DIR" -name "*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete

# Hiển thị danh sách backup hiện có
echo "----------------------------------------"
echo "Current backups:"
ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null | tail -5

echo "=========================================="
echo "Backup completed: $(date)"
echo "=========================================="
