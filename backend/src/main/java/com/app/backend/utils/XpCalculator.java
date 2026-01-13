package com.app.backend.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Utility class thống nhất tính toán XP và Level
 * 
 * Công thức: XP_total = level * (level - 1) * 75
 * - Level 1: 0 XP
 * - Level 2: 150 XP  
 * - Level 10: 6,750 XP
 * - Level 25: 45,000 XP
 * - Level 50: 183,750 XP
 * - Level 100: 742,500 XP
 */
public final class XpCalculator {

    // Hệ số điều chỉnh độ khó (tăng = khó lên level hơn)
    private static final int DIFFICULTY_MULTIPLIER = 75;
    
    // Level tối đa
    public static final int MAX_LEVEL = 100;

    private XpCalculator() {
        // Utility class - không cho khởi tạo
    }

    /**
     * Tính tổng XP cần để ĐẠT một level
     * @param level Cấp độ cần đạt
     * @return Tổng XP tích lũy cần có
     */
    public static long xpRequiredForLevel(int level) {
        if (level <= 1) return 0;
        return (long) level * (level - 1) * DIFFICULTY_MULTIPLIER;
    }

    /**
     * Tính XP cần để lên level tiếp theo (từ level hiện tại)
     * @param currentLevel Level hiện tại
     * @return XP cần để lên level tiếp theo
     */
    public static long xpNeededForNextLevel(int currentLevel) {
        if (currentLevel < 1) currentLevel = 1;
        // XP từ level N → N+1 = xpRequired(N+1) - xpRequired(N)
        // = (N+1)*N*75 - N*(N-1)*75 = 75*N*((N+1) - (N-1)) = 75*N*2 = 150*N
        return (long) currentLevel * 2 * DIFFICULTY_MULTIPLIER;
    }

    /**
     * Tính level từ tổng XP
     * @param totalXp Tổng XP hiện có
     * @return Level hiện tại
     */
    public static int calculateLevel(long totalXp) {
        if (totalXp <= 0) return 1;
        // Từ công thức: XP = level * (level - 1) * 75
        // level^2 - level - XP/75 = 0
        // level = (1 + sqrt(1 + 4*XP/75)) / 2
        double level = (1 + Math.sqrt(1 + (4.0 * totalXp) / DIFFICULTY_MULTIPLIER)) / 2.0;
        int calculatedLevel = Math.max(1, (int) Math.floor(level));
        return Math.min(calculatedLevel, MAX_LEVEL);
    }

    /**
     * Tính thông tin chi tiết level từ tổng XP
     * @param totalXp Tổng XP hiện có
     * @return LevelInfo chứa level, XP hiện tại trong level, XP cần cho level tiếp
     */
    public static LevelInfo computeLevelInfo(long totalXp) {
        if (totalXp < 0) totalXp = 0;
        
        int level = calculateLevel(totalXp);
        
        // XP đã dùng để đạt level hiện tại
        long xpForCurrentLevel = xpRequiredForLevel(level);
        // XP cần để đạt level tiếp theo
        long xpForNextLevel = xpRequiredForLevel(level + 1);
        
        // XP còn dư trong level hiện tại
        long xpInCurrentLevel = totalXp - xpForCurrentLevel;
        // XP cần để lên level tiếp
        long xpNeededForNext = xpForNextLevel - xpForCurrentLevel;
        // XP còn thiếu để lên level
        long xpToNext = xpForNextLevel - totalXp;
        
        // Phần trăm tiến độ
        double progressPercent = xpNeededForNext > 0 
            ? (double) xpInCurrentLevel / xpNeededForNext * 100.0 
            : 100.0;

        return LevelInfo.builder()
                .level(level)
                .xpInCurrentLevel(xpInCurrentLevel)
                .xpNeededForNext(xpNeededForNext)
                .xpToNextLevel(xpToNext)
                .progressPercent(Math.min(progressPercent, 100.0))
                .build();
    }

    /**
     * Class chứa thông tin level
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelInfo {
        private int level;              // Level hiện tại
        private long xpInCurrentLevel;  // XP đã có trong level này
        private long xpNeededForNext;   // Tổng XP cần để lên level (từ 0 của level hiện tại)
        private long xpToNextLevel;     // XP còn thiếu để lên level
        private double progressPercent; // % tiến độ (0-100)
    }
}
