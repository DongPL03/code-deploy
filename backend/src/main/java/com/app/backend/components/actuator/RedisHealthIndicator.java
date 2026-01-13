package com.app.backend.components.actuator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom Health Indicator để kiểm tra Redis connection
 * Hiển thị trong /actuator/health
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            // Thử ping Redis
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            String pong = connection.ping();
            
            // Lấy thông tin Redis
            var info = connection.serverCommands().info();
            
            connection.close();

            if ("PONG".equalsIgnoreCase(pong)) {
                return Health.up()
                        .withDetail("status", "Redis is reachable")
                        .withDetail("ping", pong)
                        .withDetail("info", info != null ? "Available" : "N/A")
                        .build();
            } else {
                log.error("Redis ping failed: {}", pong);
                return Health.down()
                        .withDetail("status", "Redis ping failed")
                        .withDetail("response", pong)
                        .build();
            }

        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                    .withDetail("status", "Redis is unreachable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
