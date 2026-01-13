package com.app.backend.services.bangxephang;

import com.app.backend.responses.bangxephang.LeaderboardEntryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardRedisService implements ILeaderboardRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.data.redis.use-redis-cache:false}")
    private boolean useRedisCache;

    private static final String KEY_PREFIX = "leaderboard:";
    private static final long TTL_MINUTES = 2; // 2 phút - leaderboard update thường xuyên

    /**
     * Generate cache key from params
     * Format: leaderboard:{timeRange}:{chuDeId}:{boCauHoiId}:{page}:{limit}
     */
    private String generateKey(String timeRange, Long chuDeId, Long boCauHoiId, int page, int limit) {
        return String.format("%s%s:%s:%s:%d:%d",
                KEY_PREFIX,
                timeRange != null ? timeRange : "ALL",
                chuDeId != null ? chuDeId : "0",
                boCauHoiId != null ? boCauHoiId : "0",
                page,
                limit
        );
    }

    @Override
    public Page<LeaderboardEntryResponse> getLeaderboard(String timeRange, Long chuDeId, Long boCauHoiId, int page, int limit) {
        if (!useRedisCache) {
            return null;
        }

        try {
            String key = generateKey(timeRange, chuDeId, boCauHoiId, page, limit);
            String json = (String) redisTemplate.opsForValue().get(key);

            if (json.isEmpty()) {
                log.debug("🔴 Redis cache MISS for leaderboard: {}", key);
                return null;
            }

            // Parse cached data
            CachedLeaderboardPage cached = objectMapper.readValue(json, CachedLeaderboardPage.class);
            Page<LeaderboardEntryResponse> result = new PageImpl<>(
                    cached.content,
                    PageRequest.of(page, limit),
                    cached.totalElements
            );
            
            log.debug("🟢 Redis cache HIT for leaderboard: {} ({} entries)", key, cached.content.size());
            return result;
        } catch (Exception e) {
            log.warn("⚠️ Redis read error for leaderboard: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void saveLeaderboard(String timeRange, Long chuDeId, Long boCauHoiId, int page, int limit, Page<LeaderboardEntryResponse> data) {
        if (!useRedisCache) {
            return;
        }

        try {
            String key = generateKey(timeRange, chuDeId, boCauHoiId, page, limit);
            
            // Wrap in cacheable format
            CachedLeaderboardPage cached = new CachedLeaderboardPage();
            cached.content = data.getContent();
            cached.totalElements = data.getTotalElements();
            
            String json = objectMapper.writeValueAsString(cached);
            redisTemplate.opsForValue().set(key, json, TTL_MINUTES, TimeUnit.MINUTES);
            
            log.info("✅ Redis cache SAVED for leaderboard: {} ({} entries), TTL={}min", 
                    key, data.getContent().size(), TTL_MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Redis save error for leaderboard: {}", e.getMessage());
        }
    }

    @Override
    public void clearLeaderboardCache() {
        if (!useRedisCache) {
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🗑️ Redis cache CLEARED for all leaderboards ({} keys)", keys.size());
            }
        } catch (Exception e) {
            log.warn("⚠️ Redis clear error for leaderboard: {}", e.getMessage());
        }
    }

    @Override
    public void clearLeaderboardCache(String timeRange, Long chuDeId, Long boCauHoiId) {
        if (!useRedisCache) {
            return;
        }

        try {
            // Clear all pages for this specific leaderboard configuration
            String pattern = String.format("%s%s:%s:%s:*",
                    KEY_PREFIX,
                    timeRange != null ? timeRange : "ALL",
                    chuDeId != null ? chuDeId : "0",
                    boCauHoiId != null ? boCauHoiId : "0"
            );
            
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🗑️ Redis cache CLEARED for leaderboard pattern: {} ({} keys)", pattern, keys.size());
            }
        } catch (Exception e) {
            log.warn("⚠️ Redis clear error for specific leaderboard: {}", e.getMessage());
        }
    }

    /**
     * Helper class for caching Page data
     */
    private static class CachedLeaderboardPage {
        public List<LeaderboardEntryResponse> content;
        public long totalElements;
    }
}
