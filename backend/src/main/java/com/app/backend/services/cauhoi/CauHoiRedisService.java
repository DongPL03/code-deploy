package com.app.backend.services.cauhoi;

import com.app.backend.dtos.cache.CauHoiCacheDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Redis Cache Service cho Câu Hỏi
 * 
 * Cache DTO thay vì Entity để:
 * - Tránh vấn đề lazy loading khi deserialize
 * - Dễ serialize/deserialize
 * - Code chuyên nghiệp hơn
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CauHoiRedisService implements ICauHoiRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.data.redis.use-redis-cache:false}")
    private boolean useRedisCache;

    private static final String KEY_PREFIX = "cau_hoi:bo_cau_hoi:";
    private static final Duration TTL = Duration.ofHours(1); // 1 giờ

    private String generateKey(Long boCauHoiId) {
        return KEY_PREFIX + boCauHoiId;
    }

    @Override
    public List<CauHoiCacheDTO> getQuestionsByBoCauHoiId(Long boCauHoiId) {
        if (!useRedisCache || boCauHoiId == null) {
            return null;
        }

        try {
            String key = generateKey(boCauHoiId);
            String json = (String) redisTemplate.opsForValue().get(key);

            if (json == null || json.isEmpty()) {
                log.debug("📭 Redis cache MISS for questions of boCauHoiId={}", boCauHoiId);
                return null;
            }

            List<CauHoiCacheDTO> questions = objectMapper.readValue(json, new TypeReference<List<CauHoiCacheDTO>>() {});
            log.debug("✅ Redis cache HIT for questions of boCauHoiId={}: {} questions", boCauHoiId, questions.size());
            return questions;
            
        } catch (Exception e) {
            log.warn("⚠️ Redis read error for questions: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void saveQuestionsByBoCauHoiId(Long boCauHoiId, List<CauHoiCacheDTO> questions) {
        if (!useRedisCache || boCauHoiId == null || questions == null) {
            return;
        }

        try {
            String key = generateKey(boCauHoiId);
            String json = objectMapper.writeValueAsString(questions);
            redisTemplate.opsForValue().set(key, json, TTL);
            
            log.info("✅ Redis cache SAVED for questions of boCauHoiId={}: {} questions, TTL={}", 
                    boCauHoiId, questions.size(), TTL);
                    
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Redis save error for questions: {}", e.getMessage());
        }
    }

    @Override
    public void clearCacheForBoCauHoi(Long boCauHoiId) {
        if (!useRedisCache || boCauHoiId == null) {
            return;
        }

        try {
            String key = generateKey(boCauHoiId);
            redisTemplate.delete(key);
            log.info("🗑️ Redis cache CLEARED for questions of boCauHoiId={}", boCauHoiId);
        } catch (Exception e) {
            log.warn("⚠️ Redis clear error for questions: {}", e.getMessage());
        }
    }

    @Override
    public void clearAllCache() {
        if (!useRedisCache) {
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🗑️ Redis cache CLEARED for ALL questions ({} keys)", keys.size());
            }
        } catch (Exception e) {
            log.warn("⚠️ Redis clear error for all questions: {}", e.getMessage());
        }
    }
}
