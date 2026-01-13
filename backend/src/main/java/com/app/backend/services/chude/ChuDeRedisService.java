package com.app.backend.services.chude;

import com.app.backend.models.ChuDe;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChuDeRedisService implements IChuDeRedisService {
    private static final Logger logger = LoggerFactory.getLogger(ChuDeRedisService.class);
    private static final String CACHE_KEY = "all_chu_de";
    private static final long CACHE_TTL_HOURS = 1; // Cache 1 giờ

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    @Value("${spring.data.redis.use-redis-cache:false}")
    private boolean useRedisCache;

    @Override
    public List<ChuDe> getAllChuDe() {
        if (!useRedisCache) {
            return null;
        }

        try {
            String json = (String) redisTemplate.opsForValue().get(CACHE_KEY);
            if (json == null || json.isEmpty()) {
                return null;
            }

            logger.info("✅ Redis cache HIT for: {}", CACHE_KEY);
            return redisObjectMapper.readValue(json, new TypeReference<List<ChuDe>>() {});
        } catch (Exception e) {
            logger.warn("⚠️ Redis cache error (ChuDe): {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void saveAllChuDe(List<ChuDe> chuDeList) {
        if (!useRedisCache) {
            return;
        }

        try {
            String json = redisObjectMapper.writeValueAsString(chuDeList);
            redisTemplate.opsForValue().set(CACHE_KEY, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
            logger.info("✅ Redis cache SAVED for: {} ({} items, TTL: {} hour)", CACHE_KEY, chuDeList.size(), CACHE_TTL_HOURS);
        } catch (Exception e) {
            logger.warn("⚠️ Redis save error (ChuDe): {}", e.getMessage());
        }
    }

    @Override
    public void clearChuDeCache() {
        if (!useRedisCache) {
            return;
        }

        try {
            redisTemplate.delete(CACHE_KEY);
            logger.info("🗑️ Redis cache CLEARED for: {}", CACHE_KEY);
        } catch (Exception e) {
            logger.warn("⚠️ Redis clear error (ChuDe): {}", e.getMessage());
        }
    }
}
