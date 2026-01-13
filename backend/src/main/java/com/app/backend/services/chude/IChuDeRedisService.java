package com.app.backend.services.chude;

import com.app.backend.models.ChuDe;

import java.util.List;

public interface IChuDeRedisService {
    
    /**
     * Lấy danh sách chủ đề từ Redis cache
     * @return List<ChuDe> hoặc null nếu cache miss
     */
    List<ChuDe> getAllChuDe();
    
    /**
     * Lưu danh sách chủ đề vào Redis cache
     */
    void saveAllChuDe(List<ChuDe> chuDeList);
    
    /**
     * Xóa cache chủ đề (gọi khi có thay đổi)
     */
    void clearChuDeCache();
}
