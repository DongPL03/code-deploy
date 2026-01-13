# Production Scripts

Tập hợp các scripts để quản lý, backup, và monitor hệ thống production.

## 📁 Danh sách Scripts

### 1. `backup-database.sh` - Tự động backup database

**Chức năng:**

- Backup MySQL database từ Docker container
- Nén file backup bằng gzip (tiết kiệm ~80% dung lượng)
- Tự động xóa backup cũ hơn 7 ngày
- Log chi tiết quá trình backup

**Sử dụng thủ công:**

```bash
cd backend
bash scripts/backup-database.sh
```

**Tự động backup hàng ngày (Crontab trên Linux):**

```bash
# Mở crontab editor
crontab -e

# Thêm dòng này để backup lúc 2h sáng mỗi ngày
0 2 * * * cd /path/to/backend && bash scripts/backup-database.sh >> logs/backup.log 2>&1
```

**Tự động backup trên Windows (Task Scheduler):**

1. Mở Task Scheduler
2. Create Basic Task → đặt tên "Database Backup"
3. Trigger: Daily, 2:00 AM
4. Action: Start a program
   - Program: `C:\Program Files\Git\bin\bash.exe`
   - Arguments: `scripts/backup-database.sh`
   - Start in: `E:\ute\backend`

### 2. `restore-database.sh` - Khôi phục database

**Sử dụng:**

```bash
# Xem danh sách backup
bash scripts/restore-database.sh

# Restore từ file cụ thể
bash scripts/restore-database.sh backups/mysql/dau_truong_tri_thuc_20260111_020000.sql.gz
```

### 3. `monitor-health.sh` - Giám sát health tự động

**Chức năng:**

- Kiểm tra health endpoint mỗi 5 phút
- Tự động restart service khi DOWN
- Retry 3 lần trước khi restart
- Log chi tiết quá trình monitor

**Sử dụng thủ công:**

```bash
bash scripts/monitor-health.sh
```

**Tự động monitor (Crontab):**

```bash
# Kiểm tra mỗi 5 phút
*/5 * * * * cd /path/to/backend && bash scripts/monitor-health.sh >> logs/monitor.log 2>&1
```

**Tự động monitor (Windows Task Scheduler):**

- Trigger: Repeat task every 5 minutes
- Program: `C:\Program Files\Git\bin\bash.exe`
- Arguments: `scripts/monitor-health.sh`

### 4. `manage-service.sh` - Quản lý Docker services

**Sử dụng:**

```bash
# Start tất cả services
bash scripts/manage-service.sh start

# Stop tất cả services
bash scripts/manage-service.sh stop

# Restart services
bash scripts/manage-service.sh restart

# Kiểm tra status và health
bash scripts/manage-service.sh status

# Xem logs (tất cả services)
bash scripts/manage-service.sh logs

# Xem logs của service cụ thể
bash scripts/manage-service.sh logs spring-app-container

# Rebuild và restart
bash scripts/manage-service.sh rebuild
```

## 📂 Cấu trúc thư mục

```
backend/
├── scripts/
│   ├── backup-database.sh      # Backup database
│   ├── restore-database.sh     # Restore database
│   ├── monitor-health.sh       # Monitor health tự động
│   ├── manage-service.sh       # Quản lý Docker services
│   └── README.md               # Hướng dẫn này
├── backups/
│   └── mysql/                  # Backup files
│       ├── dau_truong_tri_thuc_20260110_020000.sql.gz
│       └── ...
└── logs/
    ├── backup.log              # Backup logs
    ├── monitor.log             # Monitor logs
    └── app.log                 # Application logs
```

## ⚙️ Cấu hình

**Thay đổi thông tin database:** Sửa file `backup-database.sh` và `restore-database.sh`

```bash
CONTAINER_NAME="mysql-dautruong"
DB_NAME="dau_truong_tri_thuc"
DB_USER="root"
DB_PASSWORD="dongle170503"
RETENTION_DAYS=7              # Giữ backup trong 7 ngày
```

**Backup thường xuyên hơn (mỗi 6 giờ):**

```bash
0 */6 * * * cd /path/to/backend && bash scripts/backup-database.sh >> logs/backup.log 2>&1
```

## 🔒 Bảo mật

**⚠️ QUAN TRỌNG:** File backup chứa toàn bộ dữ liệu nhạy cảm!

1. **Không commit backup vào Git:**

   ```bash
   # Đã thêm vào .gitignore
   backups/
   *.sql
   *.sql.gz
   ```

2. **Upload backup lên cloud storage:**

   - Google Drive
   - AWS S3
   - Dropbox

   Ví dụ với rclone:

   ```bash
   # Cài rclone và cấu hình Google Drive
   rclone copy backups/mysql/ gdrive:Backups/DatabaseBackups/
   ```

3. **Encrypt backup (khuyến nghị):**
   ```bash
   # Mã hóa bằng GPG
   gpg --symmetric --cipher-algo AES256 backup.sql.gz
   ```

## 📊 Dung lượng backup ước tính

| Số bản ghi | Database size | Backup size (gzipped) |
| ---------- | ------------- | --------------------- |
| 10,000     | ~50 MB        | ~8 MB                 |
| 100,000    | ~500 MB       | ~80 MB                |
| 1,000,000  | ~5 GB         | ~800 MB               |

**Với RETENTION_DAYS=7:** Giữ 7 backup = ~560 MB disk space (cho 100k bản ghi)

## 🚨 Khắc phục sự cố

**Lỗi: "Container is not running"**

```bash
# Kiểm tra container
docker ps -a | grep mysql

# Start container
docker start mysql-dautruong
```

**Lỗi: "Permission denied"**

```bash
# Cấp quyền thực thi cho tất cả scripts
chmod +x scripts/*.sh
```

**Backup file bị lỗi:**

```bash
# Kiểm tra file có hợp lệ không
gunzip -t backups/mysql/backup.sql.gz

# Xem nội dung (10 dòng đầu)
gunzip -c backups/mysql/backup.sql.gz | head -10
```

**Health check không hoạt động:**

```bash
# Kiểm tra xem endpoint có trả về không
curl -v http://localhost:8088/actuator/health

# Kiểm tra logs
bash scripts/manage-service.sh logs spring-app-container
```

**Service không tự restart:**

```bash
# Kiểm tra monitor script có đang chạy không
ps aux | grep monitor-health.sh

# Chạy thủ công để debug
bash scripts/monitor-health.sh
```

## 🚀 Quick Start

**Setup ban đầu:**

```bash
# 1. Cấp quyền thực thi cho tất cả scripts
chmod +x scripts/*.sh

# 2. Start services
bash scripts/manage-service.sh start

# 3. Kiểm tra status
bash scripts/manage-service.sh status

# 4. Backup ngay lập tức
bash scripts/backup-database.sh

# 5. Setup crontab (Linux/Mac)
crontab -e
# Thêm 2 dòng sau:
# 0 2 * * * cd /path/to/backend && bash scripts/backup-database.sh >> logs/backup.log 2>&1
# */5 * * * * cd /path/to/backend && bash scripts/monitor-health.sh >> logs/monitor.log 2>&1
```

## 📝 Best Practices

1. **Monitoring:**
   - Kiểm tra logs thường xuyên: `tail -f logs/monitor.log`
   - Setup alert qua email/Slack khi service DOWN
2. **Backup:**
   - Backup hàng ngày lúc traffic thấp (2-4h sáng)
   - Test restore định kỳ (mỗi tháng)
   - Upload backup lên cloud storage
3. **Security:**
   - Thay đổi database password định kỳ
   - Encrypt backup files trước khi upload
   - Không để password trong script (dùng environment variables)
4. **Performance:**
   - Monitor RAM/CPU usage: `docker stats`
   - Check health metrics: `curl http://localhost:8088/actuator/metrics`
   - Optimize khi memory usage > 85%

## 📞 Support

Nếu gặp vấn đề:

1. Kiểm tra logs: `bash scripts/manage-service.sh logs`
2. Check health: `bash scripts/manage-service.sh status`
3. Review monitor logs: `tail -100 logs/monitor.log`
   gunzip -c backups/mysql/backup.sql.gz | head -10

```

```
