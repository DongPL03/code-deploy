package com.app.backend.services.bocauhoi;

import com.app.backend.dtos.cache.BoCauHoiPageCacheDTO;
import org.springframework.data.domain.PageRequest;

/**
 * Redis Cache Service cho Bộ Câu Hỏi
 * 
 * Cache DTO thay vì Entity để:
 * - Tránh vấn đề lazy loading khi deserialize
 * - Dễ serialize/deserialize
 * - Code chuyên nghiệp hơn
 */
public interface IQuestionSetRedisService {
    
    /**
     * Xóa toàn bộ cache
     */
    void clear();
    
    /**
     * Lấy danh sách bộ câu hỏi từ cache
     * 
     * @return BoCauHoiPageCacheDTO nếu cache hit, null nếu cache miss
     */
    BoCauHoiPageCacheDTO getQuestionListFromCache(
            PageRequest pageRequest,
            String keyword,
            Long chuDeId,
            String cheDoHienThi,
            String trangThai,
            String loaiSuDung,
            Boolean muonTaoTraPhi,
            Long nguoiTaoId,
            Double minRating,
            Double maxRating,
            Long creatorId,
            boolean isAdmin
    );

    /**
     * Lưu danh sách bộ câu hỏi vào cache
     * 
     * @param cacheData DTO chứa danh sách và thông tin phân trang
     */
    void saveQuestionListToCache(
            BoCauHoiPageCacheDTO cacheData,
            PageRequest pageRequest,
            String keyword,
            Long chuDeId,
            String cheDoHienThi,
            String trangThai,
            String loaiSuDung,
            Boolean muonTaoTraPhi,
            Long nguoiTaoId,
            Double minRating,
            Double maxRating,
            Long creatorId,
            boolean isAdmin
    );
    
    /**
     * Xóa cache liên quan đến bộ câu hỏi (khi có thay đổi)
     */
    void invalidateQuestionListCache();
}
