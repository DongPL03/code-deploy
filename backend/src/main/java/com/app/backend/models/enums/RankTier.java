package com.app.backend.models.enums;

public enum RankTier {
    DONG(0, 1.0),           // Đồng
    BAC(2000, 1.05),        // Bạc
    VANG(5000, 1.10),       // Vàng
    BACH_KIM(10000, 1.15),  // Bạch Kim
    KIM_CUONG(20000, 1.20), // Kim Cương
    CAO_THU(35000, 1.25);   // Cao Thủ

    private final int minPoints;
    private final double multiplier;

    RankTier(int minPoints, double multiplier) {
        this.minPoints = minPoints;
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }

    // Hàm tiện ích để tìm Rank dựa trên điểm
    public static RankTier fromPoints(int points) {
        // Duyệt ngược từ cao xuống thấp để tìm rank phù hợp
        RankTier[] tiers = values();
        for (int i = tiers.length - 1; i >= 0; i--) {
            if (points >= tiers[i].minPoints) {
                return tiers[i];
            }
        }
        return DONG;
    }
}