package com.app.backend.configurations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Cấu hình Async Thread Pool cho VPS 4GB RAM (2 vCPU)
 * 
 * Mục đích:
 * - Quản lý thread pool cho @Async operations
 * - Tối ưu cho WebSocket broadcasting, notifications
 * - Tránh tạo quá nhiều thread gây OOM
 * 
 * Thread pools:
 * 1. taskExecutor (default) - General async tasks
 * 2. wsExecutor - WebSocket message broadcasting  
 * 3. notificationExecutor - Push notifications
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    // ==================== CONFIGURABLE VALUES ====================
    
    @Value("${async.executor.core-pool-size:4}")
    private int corePoolSize;
    
    @Value("${async.executor.max-pool-size:8}")
    private int maxPoolSize;
    
    @Value("${async.executor.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${async.executor.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    // ==================== DEFAULT TASK EXECUTOR ====================
    
    /**
     * Default executor cho @Async không chỉ định executor name
     * Dùng cho: Battle loop, general background tasks
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core threads luôn active (2 vCPU → 4 core threads)
        executor.setCorePoolSize(corePoolSize);
        
        // Max threads khi queue đầy (giới hạn để tránh OOM)
        executor.setMaxPoolSize(maxPoolSize);
        
        // Queue chứa tasks chờ xử lý
        executor.setQueueCapacity(queueCapacity);
        
        // Thời gian thread idle trước khi bị thu hồi
        executor.setKeepAliveSeconds(keepAliveSeconds);
        
        // Thread name prefix cho debugging
        executor.setThreadNamePrefix("async-task-");
        
        // Cho phép core threads timeout (giải phóng RAM khi idle)
        executor.setAllowCoreThreadTimeOut(true);
        
        // Chờ tasks hoàn thành khi shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        // Rejection policy khi queue đầy
        executor.setRejectedExecutionHandler(new LoggingRejectedHandler("taskExecutor"));
        
        executor.initialize();
        log.info("✅ TaskExecutor initialized: core={}, max={}, queue={}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }

    // ==================== WEBSOCKET EXECUTOR ====================
    
    /**
     * Executor riêng cho WebSocket broadcasting
     * Cần response nhanh, không nên chờ queue dài
     * 
     * Dùng: @Async("wsExecutor")
     */
    @Bean(name = "wsExecutor")
    public Executor wsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // WebSocket cần phản hồi nhanh → nhiều core threads hơn
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        
        // Queue nhỏ hơn - WebSocket messages không nên chờ lâu
        executor.setQueueCapacity(50);
        
        executor.setKeepAliveSeconds(30); // Thu hồi nhanh hơn
        executor.setThreadNamePrefix("ws-broadcast-");
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        
        // Nếu queue đầy → chạy trực tiếp trên caller thread (đảm bảo không mất message)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        log.info("✅ WebSocket Executor initialized: core={}, max={}, queue=50", 
                corePoolSize, maxPoolSize);
        
        return executor;
    }

    // ==================== NOTIFICATION EXECUTOR ====================
    
    /**
     * Executor cho push notifications
     * Có thể chờ queue dài hơn vì không cần realtime
     * 
     * Dùng: @Async("notificationExecutor")
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Notification có thể chờ → ít core threads
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        
        // Queue lớn hơn - notifications có thể batch
        executor.setQueueCapacity(200);
        
        executor.setKeepAliveSeconds(120); // Giữ thread lâu hơn
        executor.setThreadNamePrefix("notification-");
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        // Discard oldest nếu queue đầy (notification cũ ít quan trọng hơn)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        
        executor.initialize();
        log.info("✅ Notification Executor initialized: core=2, max=4, queue=200");
        
        return executor;
    }

    // ==================== CUSTOM REJECTION HANDLER ====================
    
    /**
     * Custom handler để log khi task bị reject
     */
    private static class LoggingRejectedHandler implements RejectedExecutionHandler {
        private final String executorName;
        
        public LoggingRejectedHandler(String executorName) {
            this.executorName = executorName;
        }
        
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("⚠️ Task rejected from {}: pool={}/{}, queue={}/{}", 
                    executorName,
                    executor.getActiveCount(),
                    executor.getMaximumPoolSize(),
                    executor.getQueue().size(),
                    executor.getQueue().remainingCapacity() + executor.getQueue().size());
            
            // Fallback: chạy trên caller thread thay vì bỏ task
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }
}
