package com.app.backend.components.actuator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Custom Health Indicator để kiểm tra bộ nhớ của ứng dụng
 * Hiển thị trong /actuator/health
 */
@Component
@Slf4j
public class MemoryHealthIndicator implements HealthIndicator {

    private static final long MEMORY_WARNING_THRESHOLD = 90; // 90% memory usage -> WARNING
    private static final long MEMORY_CRITICAL_THRESHOLD = 95; // 95% memory usage -> DOWN

    @Override
    public Health health() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            
            long usedMemory = heapUsage.getUsed();
            long maxMemory = heapUsage.getMax();
            long usedPercentage = (usedMemory * 100) / maxMemory;

            // Build health details
            Health.Builder healthBuilder = Health.up()
                    .withDetail("used", formatBytes(usedMemory))
                    .withDetail("max", formatBytes(maxMemory))
                    .withDetail("usage", usedPercentage + "%")
                    .withDetail("committed", formatBytes(heapUsage.getCommitted()));

            // Kiểm tra ngưỡng
            if (usedPercentage >= MEMORY_CRITICAL_THRESHOLD) {
                log.error("CRITICAL: Memory usage at {}% ({}MB / {}MB)", 
                        usedPercentage, 
                        usedMemory / (1024 * 1024), 
                        maxMemory / (1024 * 1024));
                return healthBuilder
                        .down()
                        .withDetail("reason", "Memory usage critical: " + usedPercentage + "%")
                        .build();
            } else if (usedPercentage >= MEMORY_WARNING_THRESHOLD) {
                log.warn("WARNING: Memory usage at {}% ({}MB / {}MB)", 
                        usedPercentage, 
                        usedMemory / (1024 * 1024), 
                        maxMemory / (1024 * 1024));
                return healthBuilder
                        .status("WARNING")
                        .withDetail("reason", "Memory usage high: " + usedPercentage + "%")
                        .build();
            }

            return healthBuilder.build();

        } catch (Exception e) {
            log.error("Error checking memory health", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private String formatBytes(long bytes) {
        long mb = bytes / (1024 * 1024);
        return mb + " MB";
    }
}
