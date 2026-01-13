package com.app.backend.models;

import com.app.backend.services.bocauhoi.IQuestionSetRedisService;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Entity Listener để tự động clear Redis cache khi BoCauHoi thay đổi.
 * Sử dụng static ApplicationContext vì JPA không hỗ trợ DI trực tiếp.
 */
@Component
public class QuestionSetListener {
    
    private static final Logger logger = LoggerFactory.getLogger(QuestionSetListener.class);
    
    private static ApplicationContext applicationContext;
    
    @Autowired
    public void setApplicationContext(ApplicationContext ctx) {
        QuestionSetListener.applicationContext = ctx;
    }
    
    private IQuestionSetRedisService getRedisService() {
        if (applicationContext == null) {
            logger.warn("ApplicationContext chưa được khởi tạo");
            return null;
        }
        return applicationContext.getBean(IQuestionSetRedisService.class);
    }

    @PostPersist
    public void postPersist(BoCauHoi boCauHoi) {
        logger.info("🔄 BoCauHoi CREATED: {} - clearing Redis cache", boCauHoi.getId());
        clearCache();
    }

    @PostUpdate
    public void postUpdate(BoCauHoi boCauHoi) {
        logger.info("🔄 BoCauHoi UPDATED: {} - clearing Redis cache", boCauHoi.getId());
        clearCache();
    }

    @PostRemove
    public void postRemove(BoCauHoi boCauHoi) {
        logger.info("🔄 BoCauHoi DELETED: {} - clearing Redis cache", boCauHoi.getId());
        clearCache();
    }
    
    private void clearCache() {
        try {
            IQuestionSetRedisService redisService = getRedisService();
            if (redisService != null) {
                // Chỉ invalidate cache liên quan đến danh sách bộ câu hỏi
                // Không xóa toàn bộ cache (tối ưu hơn)
                redisService.invalidateQuestionListCache();
            }
        } catch (Exception e) {
            logger.warn("⚠️ Không thể invalidate Redis cache: {}", e.getMessage());
        }
    }
}
