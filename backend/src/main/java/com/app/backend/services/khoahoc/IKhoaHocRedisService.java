package com.app.backend.services.khoahoc;

import com.app.backend.models.KhoaHoc;
import org.springframework.data.domain.Page;

public interface IKhoaHocRedisService {
    /**
     * Get cached course list
     * @param keyword search keyword
     * @param chuDeId topic ID
     * @param trangThai status
     * @param page page number
     * @param limit items per page
     * @param sortOrder sort order
     * @return cached Page or null if miss
     */
    Page<KhoaHoc> getKhoaHocList(String keyword, Long chuDeId, String trangThai, 
                                  Double minRating, Double maxRating,
                                  int page, int limit, String sortOrder);

    /**
     * Cache course list result
     */
    void saveKhoaHocList(String keyword, Long chuDeId, String trangThai,
                         Double minRating, Double maxRating,
                         int page, int limit, String sortOrder, Page<KhoaHoc> data);

    /**
     * Clear all course cache (call when courses are modified)
     */
    void clearKhoaHocCache();
}
