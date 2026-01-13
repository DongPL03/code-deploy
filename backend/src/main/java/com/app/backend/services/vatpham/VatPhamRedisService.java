package com.app.backend.services.vatpham;

import com.app.backend.models.VatPham;
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
public class VatPhamRedisService implements IVatPhamRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.data.redis.use-redis-cache:false}")
    private boolean useRedisCache;

    private static final String ALL_ACTIVE_ITEMS_KEY = "vat_pham:all_active";
    private static final long TTL_MINUTES = 30; // 30 phút cho shop items

    @Override
    public List<VatPham> getAllActiveItems() {
        if (!useRedisCache) {
            return null;
        }

        try {
            String json = (String) redisTemplate.opsForValue().get(ALL_ACTIVE_ITEMS_KEY);

            if (json == null || json.isEmpty()) {
                log.debug("🔴 Redis cache MISS for all active items");
                return null;
            }

            List<VatPham> items = objectMapper.readValue(json, new TypeReference<List<VatPham>>() {});
            log.debug("🟢 Redis cache HIT for all active items: {} items", items.size());
            return items;
        } catch (Exception e) {
            log.warn("⚠️ Redis read error for items: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void saveAllActiveItems(List<VatPham> items) {
        if (!useRedisCache) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(items);
            redisTemplate.opsForValue().set(ALL_ACTIVE_ITEMS_KEY, json, TTL_MINUTES, TimeUnit.MINUTES);
            log.info("✅ Redis cache SAVED for all active items: {} items, TTL={}min", items.size(), TTL_MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Redis save error for items: {}", e.getMessage());
        }
    }

    @Override
    public void clearItemsCache() {
        if (!useRedisCache) {
            return;
        }

        try {
            redisTemplate.delete(ALL_ACTIVE_ITEMS_KEY);
            log.info("🗑️ Redis cache CLEARED for items");
        } catch (Exception e) {
            log.warn("⚠️ Redis clear error for items: {}", e.getMessage());
        }
    }
}
