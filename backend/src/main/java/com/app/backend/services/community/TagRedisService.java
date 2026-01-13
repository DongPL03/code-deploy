package com.app.backend.services.community;

import com.app.backend.responses.community.TagResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagRedisService implements ITagRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.data.redis.use-redis-cache:false}")
    private boolean useRedisCache;

    private static final String ALL_TAGS_KEY = "tags:all_public";
    private static final long TTL_HOURS = 2; // 2 giờ - tags ít thay đổi

    @Override
    public List<TagResponse> getAllTags() {
        if (!useRedisCache) {
            return null;
        }

        try {
            String json = (String) redisTemplate.opsForValue().get(ALL_TAGS_KEY);

            if (json == null || json.isEmpty()) {
                log.debug("🔴 Redis cache MISS for tags");
                return null;
            }

            List<TagResponse> tags = objectMapper.readValue(json, new TypeReference<List<TagResponse>>() {});
            log.debug("🟢 Redis cache HIT for tags: {} items", tags.size());
            return tags;
        } catch (Exception e) {
            log.warn("⚠️ Redis read error for tags: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void saveAllTags(List<TagResponse> tags) {
        if (!useRedisCache) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(tags);
            redisTemplate.opsForValue().set(ALL_TAGS_KEY, json, TTL_HOURS, TimeUnit.HOURS);
            log.info("✅ Redis cache SAVED for tags: {} items, TTL={}h", tags.size(), TTL_HOURS);
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Redis save error for tags: {}", e.getMessage());
        }
    }

    @Override
    public void clearTagCache() {
        if (!useRedisCache) {
            return;
        }

        try {
            redisTemplate.delete(ALL_TAGS_KEY);
            log.info("🗑️ Redis cache CLEARED for tags");
        } catch (Exception e) {
            log.warn("⚠️ Redis clear error for tags: {}", e.getMessage());
        }
    }
}
