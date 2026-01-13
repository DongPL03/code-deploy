package com.app.backend.configurations;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Configuration sử dụng Bucket4j
 * 
 * Chiến lược:
 * - Mỗi user/IP có 1 bucket riêng
 * - Token được refill theo thời gian
 * - Khi hết token → reject request
 * 
 * Các tier:
 * 1. Global: Tất cả requests (cao nhất)
 * 2. Auth: Login/Register (nghiêm ngặt nhất - chống brute force)
 * 3. API: Các API thông thường
 * 4. Upload: Upload file (giới hạn thấp)
 */
@Slf4j
@Configuration
public class RateLimitConfig {

    private final StringRedisTemplate redisTemplate;

    // In-memory cache cho buckets (fallback khi Redis không khả dụng)
    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    // ==================== CONFIGURABLE LIMITS ====================
    
    @Value("${rate-limit.auth.capacity:5}")
    private int authCapacity;
    
    @Value("${rate-limit.auth.refill-tokens:5}")
    private int authRefillTokens;
    
    @Value("${rate-limit.auth.refill-minutes:1}")
    private int authRefillMinutes;
    
    @Value("${rate-limit.api.capacity:60}")
    private int apiCapacity;
    
    @Value("${rate-limit.api.refill-tokens:60}")
    private int apiRefillTokens;
    
    @Value("${rate-limit.api.refill-minutes:1}")
    private int apiRefillMinutes;
    
    @Value("${rate-limit.upload.capacity:10}")
    private int uploadCapacity;
    
    @Value("${rate-limit.upload.refill-tokens:10}")
    private int uploadRefillTokens;
    
    @Value("${rate-limit.upload.refill-minutes:5}")
    private int uploadRefillMinutes;

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    public RateLimitConfig(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Kiểm tra rate limit có được bật không
     */
    public boolean isEnabled() {
        return rateLimitEnabled;
    }

    /**
     * Lấy hoặc tạo bucket cho Auth endpoints (Login/Register)
     * Giới hạn nghiêm ngặt: 5 requests/phút
     */
    public Bucket getAuthBucket(String key) {
        String bucketKey = "rate:auth:" + key;
        return localBuckets.computeIfAbsent(bucketKey, k -> 
            Bucket.builder()
                .addLimit(Bandwidth.classic(
                    authCapacity, 
                    Refill.greedy(authRefillTokens, Duration.ofMinutes(authRefillMinutes))
                ))
                .build()
        );
    }

    /**
     * Lấy hoặc tạo bucket cho API endpoints thông thường
     * Giới hạn: 60 requests/phút
     */
    public Bucket getApiBucket(String key) {
        String bucketKey = "rate:api:" + key;
        return localBuckets.computeIfAbsent(bucketKey, k -> 
            Bucket.builder()
                .addLimit(Bandwidth.classic(
                    apiCapacity, 
                    Refill.greedy(apiRefillTokens, Duration.ofMinutes(apiRefillMinutes))
                ))
                .build()
        );
    }

    /**
     * Lấy hoặc tạo bucket cho Upload endpoints
     * Giới hạn: 10 uploads/5 phút
     */
    public Bucket getUploadBucket(String key) {
        String bucketKey = "rate:upload:" + key;
        return localBuckets.computeIfAbsent(bucketKey, k -> 
            Bucket.builder()
                .addLimit(Bandwidth.classic(
                    uploadCapacity, 
                    Refill.greedy(uploadRefillTokens, Duration.ofMinutes(uploadRefillMinutes))
                ))
                .build()
        );
    }

    /**
     * Lấy hoặc tạo bucket với custom limit
     */
    public Bucket getCustomBucket(String key, int capacity, int refillTokens, Duration refillDuration) {
        String bucketKey = "rate:custom:" + key;
        return localBuckets.computeIfAbsent(bucketKey, k -> 
            Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.greedy(refillTokens, refillDuration)))
                .build()
        );
    }

    /**
     * Xóa bucket (khi user logout hoặc để reset limit)
     */
    public void removeBucket(String key) {
        localBuckets.remove("rate:auth:" + key);
        localBuckets.remove("rate:api:" + key);
        localBuckets.remove("rate:upload:" + key);
    }

    /**
     * Lấy số tokens còn lại trong bucket
     */
    public long getRemainingTokens(Bucket bucket) {
        return bucket.getAvailableTokens();
    }
}
