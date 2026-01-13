#!/bin/bash

# ========== MySQL Restore Script cho Docker ==========
# Khôi phục database từ file backup

# Cấu hình
CONTAINER_NAME="mysql-dautruong"
DB_NAME="dau_truong_tri_thuc"
DB_USER="root"
DB_PASSWORD="dongle170503"
BACKUP_DIR="./backups/mysql"

# Kiểm tra tham số
if [ -z "$1" ]; then
    echo "Usage: $0 <backup_file.sql.gz>"
    echo ""
    echo "Available backups:"
    ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null
    exit 1
fi

BACKUP_FILE="$1"

# Kiểm tra file backup có tồn tại không
if [ ! -f "$BACKUP_FILE" ]; then
    echo "ERROR: Backup file not found: $BACKUP_FILE"
    exit 1
fi

# Kiểm tra container có đang chạy không
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "ERROR: Container $CONTAINER_NAME is not running!"
    exit 1
fi

echo "=========================================="
echo "Starting MySQL Restore: $(date)"
echo "File: $BACKUP_FILE"
echo "=========================================="

# Xác nhận từ user
read -p "⚠️  This will OVERWRITE the current database. Continue? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "Restore cancelled."
    exit 0
fi

# Thực hiện restore
echo "Restoring database: $DB_NAME"
gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER_NAME" mysql \
    -u"$DB_USER" \
    -p"$DB_PASSWORD" \
    "$DB_NAME"

# Kiểm tra restore có thành công không
if [ $? -eq 0 ]; then
    echo "✓ Restore successful!"
else
    echo "✗ Restore failed!"
    exit 1
fi

echo "=========================================="
echo "Restore completed: $(date)"
echo "=========================================="
