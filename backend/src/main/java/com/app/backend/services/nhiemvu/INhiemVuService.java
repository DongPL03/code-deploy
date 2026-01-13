package com.app.backend.services.nhiemvu;

import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.models.enums.MaNhiemVu;
import com.app.backend.responses.NhiemVuResponse;
import com.app.backend.responses.NhanThuongNhiemVuResponse;

public interface INhiemVuService {

    /**
     * Lấy danh sách nhiệm vụ của user (daily + weekly)
     */
    NhiemVuResponse getQuests(Long userId);

    /**
     * Nhận thưởng nhiệm vụ đã hoàn thành
     */
    NhanThuongNhiemVuResponse claimReward(Long userId, MaNhiemVu maNhiemVu) throws DataNotFoundException;

    /**
     * Nhận tất cả thưởng nhiệm vụ đã hoàn thành
     */
    NhanThuongNhiemVuResponse claimAllRewards(Long userId) throws DataNotFoundException;

    // ============ CẬP NHẬT TIẾN ĐỘ (gọi từ các service khác) ============

    /**
     * Cập nhật tiến độ: Tham gia trận đấu
     */
    void onMatchPlayed(Long userId, boolean isRanked);

    /**
     * Cập nhật tiến độ: Thắng trận
     */
    void onMatchWon(Long userId, boolean isRanked);

    /**
     * Cập nhật tiến độ: Trả lời đúng
     */
    void onCorrectAnswer(Long userId, int count);

    /**
     * Cập nhật tiến độ: Đạt combo
     */
    void onComboAchieved(Long userId, int comboCount);

    /**
     * Cập nhật tiến độ: Đạt Top 3
     */
    void onTop3Achieved(Long userId);
}
