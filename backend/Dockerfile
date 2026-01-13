# --- Stage 1: Build file .jar bằng Maven (Dùng JDK 21) ---
# Sử dụng Maven kèm JDK 21 của Eclipse Temurin
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy file cấu hình maven và source code
COPY pom.xml .
COPY src ./src

# Build dự án (bỏ qua test để tiết kiệm thời gian)
RUN mvn clean package -DskipTests

# --- Stage 2: Tạo môi trường chạy (Runtime - Dùng JRE 21) ---
# Dùng bản Alpine cho nhẹ (chỉ khoảng hơn 100MB)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file .jar đã build từ Stage 1 sang
COPY --from=build /app/target/*.jar app.jar

# Tạo thư mục để lưu ảnh upload
RUN mkdir -p /app/uploads

# Mở cổng 8088
EXPOSE 8088

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]