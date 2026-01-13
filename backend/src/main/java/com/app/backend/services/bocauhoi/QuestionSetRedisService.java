package com.app.backend.services.bocauhoi;

import com.app.backend.dtos.cache.BoCauHoiPageCacheDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Redis Cache Service cho Bộ Câu Hỏi
 * 
 * Cache DTO thay vì Entity để:
 * - Tránh vấn đề lazy loading khi deserialize
 * - Dễ serialize/deserialize
 * - Code chuyên nghiệp hơn
 */
@Service
@RequiredArgsConstructor
public class QuestionSetRedisService implements IQuestionSetRedisService {
    
    private static final Logger logger = LoggerFactory.getLogger(QuestionSetRedisService.class);
    private static final String CACHE_PREFIX = "question_set:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10); // TTL 10 phút

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    @Value("${spring.data.redis.use-redis-cache:false}")
    private boolean useRedisCache;

    /**
     * Tạo cache key từ các tham số filter
     */
    private String buildCacheKey(PageRequest pageRequest, String keyword, Long chuDeId,
                                  String cheDoHienThi, String trangThai, String loaiSuDung,
                                  Boolean muonTaoTraPhi, Long nguoiTaoId, Double minRating,
                                  Double maxRating, Long creatorId, boolean isAdmin) {

        int pageNumber = pageRequest.getPageNumber();
        int pageSize = pageRequest.getPageSize();
        String sortStr = pageRequest.getSort().toString().replace(": ", "_");

        return String.format("%slist:%s:%d:%s:%s:%s:%s:%d:%s:%s:%d:%b:%d:%d:%s",
                CACHE_PREFIX,
                keyword != null ? keyword : "",
                chuDeId != null ? chuDeId : 0,
                cheDoHienThi != null ? cheDoHienThi : "",
                trangThai != null ? trangThai : "",
                loaiSuDung != null ? loaiSuDung : "",
                muonTaoTraPhi,
                nguoiTaoId != null ? nguoiTaoId : 0,
                minRating,
                maxRating,
                creatorId != null ? creatorId : 0,
                isAdmin,
                pageNumber,
                pageSize,
                sortStr);
    }

    @Override
    public BoCauHoiPageCacheDTO getQuestionListFromCache(PageRequest pageRequest, String keyword,
                                                          Long chuDeId, String cheDoHienThi,
                                                          String trangThai, String loaiSuDung,
                                                          Boolean muonTaoTraPhi, Long nguoiTaoId,
                                                          Double minRating, Double maxRating,
                                                          Long creatorId, boolean isAdmin) {
        if (!useRedisCache) {
            return null;
        }
        
        try {
            String key = buildCacheKey(pageRequest, keyword, chuDeId, cheDoHienThi, trangThai,
                    loaiSuDung, muonTaoTraPhi, nguoiTaoId, minRating, maxRating, creatorId, isAdmin);

            String json = (String) redisTemplate.opsForValue().get(key);

            if (json == null || json.isEmpty()) {
                logger.debug("📭 Redis cache MISS for key: {}", key);
                return null;
            }
            
            logger.info("✅ Redis cache HIT for key: {}", key);
            return redisObjectMapper.readValue(json, BoCauHoiPageCacheDTO.class);
            
        } catch (Exception e) {
            logger.warn("⚠️ Redis cache read error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void saveQuestionListToCache(BoCauHoiPageCacheDTO cacheData, PageRequest pageRequest,
                                         String keyword, Long chuDeId, String cheDoHienThi,
                                         String trangThai, String loaiSuDung, Boolean muonTaoTraPhi,
                                         Long nguoiTaoId, Double minRating, Double maxRating,
                                         Long creatorId, boolean isAdmin) {
        if (!useRedisCache || cacheData == null) {
            return;
        }
        
        try {
            String key = buildCacheKey(pageRequest, keyword, chuDeId, cheDoHienThi, trangThai,
                    loaiSuDung, muonTaoTraPhi, nguoiTaoId, minRating, maxRating, creatorId, isAdmin);

            String json = redisObjectMapper.writeValueAsString(cacheData);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
            
            logger.info("✅ Redis cache SAVED for key: {} (TTL: {})", key, CACHE_TTL);
            
        } catch (Exception e) {
            logger.warn("⚠️ Redis cache save error: {}", e.getMessage());
        }
    }

    @Override
    public void invalidateQuestionListCache() {
        if (!useRedisCache) {
            return;
        }
        
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "list:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("🗑️ Invalidated {} question list cache entries", keys.size());
            }
        } catch (Exception e) {
            logger.warn("⚠️ Redis cache invalidate error: {}", e.getMessage());
        }
    }

    @Override
    public void clear() {
        if (!useRedisCache) {
            return;
        }
        
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("🗑️ Redis cache CLEARED: {} keys deleted", keys.size());
            }
        } catch (Exception e) {
            logger.warn("⚠️ Redis clear error: {}", e.getMessage());
        }
    }
}
