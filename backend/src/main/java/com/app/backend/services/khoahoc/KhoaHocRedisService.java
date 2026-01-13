package com.app.backend.services.khoahoc;

import com.app.backend.models.KhoaHoc;
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
public class KhoaHocRedisService implements IKhoaHocRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.data.redis.use-redis-cache:false}")
    private boolean useRedisCache;

    private static final String KEY_PREFIX = "khoa_hoc:list:";
    private static final long TTL_MINUTES = 15; // 15 phút cho danh sách khóa học

    /**
     * Generate cache key from params
     */
    private String generateKey(String keyword, Long chuDeId, String trangThai,
                               Double minRating, Double maxRating,
                               int page, int limit, String sortOrder) {
        return String.format("%s%s:%s:%s:%s:%s:%d:%d:%s",
                KEY_PREFIX,
                keyword != null ? keyword.hashCode() : "0",
                chuDeId != null && chuDeId > 0 ? chuDeId : "0",
                trangThai != null && !trangThai.isEmpty() ? trangThai : "ALL",
                minRating != null ? minRating : "0",
                maxRating != null ? maxRating : "0",
                page,
                limit,
                sortOrder != null ? sortOrder : "NEWEST"
        );
    }

    @Override
    public Page<KhoaHoc> getKhoaHocList(String keyword, Long chuDeId, String trangThai,
                                         Double minRating, Double maxRating,
                                         int page, int limit, String sortOrder) {
        if (!useRedisCache) {
            return null;
        }

        try {
            String key = generateKey(keyword, chuDeId, trangThai, minRating, maxRating, page, limit, sortOrder);
            String json = (String) redisTemplate.opsForValue().get(key);

            if (json == null || json.isEmpty()) {
                log.debug("🔴 Redis cache MISS for khoa_hoc: {}", key);
                return null;
            }

            CachedKhoaHocPage cached = objectMapper.readValue(json, CachedKhoaHocPage.class);
            Page<KhoaHoc> result = new PageImpl<>(
                    cached.content,
                    PageRequest.of(page, limit),
                    cached.totalElements
            );
            
            log.debug("🟢 Redis cache HIT for khoa_hoc: {} ({} items)", key, cached.content.size());
            return result;
        } catch (Exception e) {
            log.warn("⚠️ Redis read error for khoa_hoc: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void saveKhoaHocList(String keyword, Long chuDeId, String trangThai,
                                Double minRating, Double maxRating,
                                int page, int limit, String sortOrder, Page<KhoaHoc> data) {
        if (!useRedisCache) {
            return;
        }

        try {
            String key = generateKey(keyword, chuDeId, trangThai, minRating, maxRating, page, limit, sortOrder);
            
            CachedKhoaHocPage cached = new CachedKhoaHocPage();
            cached.content = data.getContent();
            cached.totalElements = data.getTotalElements();
            
            String json = objectMapper.writeValueAsString(cached);
            redisTemplate.opsForValue().set(key, json, TTL_MINUTES, TimeUnit.MINUTES);
            
            log.info("✅ Redis cache SAVED for khoa_hoc: {} ({} items), TTL={}min", 
                    key, data.getContent().size(), TTL_MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Redis save error for khoa_hoc: {}", e.getMessage());
        }
    }

    @Override
    public void clearKhoaHocCache() {
        if (!useRedisCache) {
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🗑️ Redis cache CLEARED for khoa_hoc ({} keys)", keys.size());
            }
        } catch (Exception e) {
            log.warn("⚠️ Redis clear error for khoa_hoc: {}", e.getMessage());
        }
    }

    /**
     * Helper class for caching Page data
     */
    private static class CachedKhoaHocPage {
        public List<KhoaHoc> content;
        public long totalElements;
    }
}
