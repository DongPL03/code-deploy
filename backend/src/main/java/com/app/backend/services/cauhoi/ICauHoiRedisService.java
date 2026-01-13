package com.app.backend.services.cauhoi;

import com.app.backend.dtos.cache.CauHoiCacheDTO;
import java.util.List;

/**
 * Redis Cache Service cho Câu Hỏi
 * 
 * Cache DTO thay vì Entity để:
 * - Tránh vấn đề lazy loading khi deserialize
 * - Dễ serialize/deserialize
 * - Code chuyên nghiệp hơn
 */
public interface ICauHoiRedisService {
    /**
     * Lấy danh sách câu hỏi theo bộ câu hỏi ID từ cache
     * @param boCauHoiId ID bộ câu hỏi
     * @return List<CauHoiCacheDTO> hoặc null nếu cache miss
     */
    List<CauHoiCacheDTO> getQuestionsByBoCauHoiId(Long boCauHoiId);

    /**
     * Lưu danh sách câu hỏi vào cache
     * @param boCauHoiId ID bộ câu hỏi
     * @param questions danh sách câu hỏi (DTO)
     */
    void saveQuestionsByBoCauHoiId(Long boCauHoiId, List<CauHoiCacheDTO> questions);

    /**
     * Xóa cache của một bộ câu hỏi (gọi khi có thay đổi)
     * @param boCauHoiId ID bộ câu hỏi
     */
    void clearCacheForBoCauHoi(Long boCauHoiId);

    /**
     * Xóa toàn bộ cache câu hỏi
     */
    void clearAllCache();
}
