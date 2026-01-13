package com.app.backend.services.loginstreak;

import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.responses.LoginStreakResponse;

public interface ILoginStreakService {

    /**
     * Lấy thông tin chuỗi đăng nhập của user
     */
    LoginStreakResponse getLoginStreakInfo(Long userId) throws DataNotFoundException;

    /**
     * Điểm danh và nhận thưởng hôm nay
     */
    LoginStreakResponse claimDailyReward(Long userId) throws DataNotFoundException;

    /**
     * Kiểm tra và cập nhật streak khi user đăng nhập
     * Được gọi tự động khi user login
     */
    void checkAndUpdateStreak(Long userId) throws DataNotFoundException;
}
