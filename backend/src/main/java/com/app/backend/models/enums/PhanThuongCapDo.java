package com.app.backend.models.enums;

import lombok.Getter;

/**
 * Định nghĩa phần thưởng lên cấp - FIX CỨNG trong code
 * Không cần lưu DB - tối ưu hiệu suất
 * 
 * Công thức XP: level N cần N * (N-1) * 50 XP tổng cộng
 * Level 1: 0 XP, Level 2: 100 XP, Level 3: 300 XP, Level 4: 600 XP...
 */
@Getter
public enum PhanThuongCapDo {
    // ============ MILESTONE ĐẶC BIỆT ============
    // Cấp 10, 20, 30, 40, 60, 70, 80, 90: Gold + vật phẩm thường
    LEVEL_10(10, 250, LoaiVatPham.GOI_Y_50_50, 2, true, "🎉 Milestone cấp 10!"),
    LEVEL_20(20, 300, LoaiVatPham.X2_DIEM, 2, true, "🎉 Milestone cấp 20!"),
    LEVEL_30(30, 350, LoaiVatPham.X2_DIEM, 2, true, "🎉 Milestone cấp 30!"),
    LEVEL_40(40, 400, LoaiVatPham.BO_QUA_CAU_HOI, 2, true, "🎉 Milestone cấp 40!"),
    LEVEL_60(60, 500, LoaiVatPham.GOI_Y_50_50, 3, true, "🎉 Milestone cấp 60!"),
    LEVEL_70(70, 550, LoaiVatPham.X2_DIEM, 3, true, "🎉 Milestone cấp 70!"),
    LEVEL_80(80, 600, LoaiVatPham.X2_DIEM, 3, true, "🎉 Milestone cấp 80!"),
    LEVEL_90(90, 650, LoaiVatPham.BO_QUA_CAU_HOI, 3, true, "🎉 Milestone cấp 90!"),

    // ============ ĐẠI MILESTONE (25, 50, 75) ============
    LEVEL_25(25, 500, LoaiVatPham.KHIEN_BAO_VE, 2, true, "🏆 Đại milestone cấp 25!"),
    LEVEL_50(50, 750, LoaiVatPham.KHIEN_BAO_VE, 3, true, "🏆 Đại milestone cấp 50!"),
    LEVEL_75(75, 1000, LoaiVatPham.X3_DIEM, 2, true, "🏆 Đại milestone cấp 75!"),

    // ============ MAX LEVEL ============
    LEVEL_100(100, 2000, LoaiVatPham.X3_DIEM, 5, true, "👑 Chúc mừng đạt cấp tối đa!");

    private final int capDo;
    private final int xuThuong;
    private final LoaiVatPham vatPhamLoai;  // Loại vật phẩm thưởng
    private final int soLuongVatPham;
    private final boolean laMilestone;
    private final String moTa;

    PhanThuongCapDo(int capDo, int xuThuong, LoaiVatPham vatPhamLoai, int soLuongVatPham, boolean laMilestone, String moTa) {
        this.capDo = capDo;
        this.xuThuong = xuThuong;
        this.vatPhamLoai = vatPhamLoai;
        this.soLuongVatPham = soLuongVatPham;
        this.laMilestone = laMilestone;
        this.moTa = moTa;
    }

    /**
     * Tìm phần thưởng cho cấp độ cụ thể
     * @return null nếu không phải milestone
     */
    public static PhanThuongCapDo findByLevel(int level) {
        for (PhanThuongCapDo pt : values()) {
            if (pt.getCapDo() == level) {
                return pt;
            }
        }
        return null;
    }

    /**
     * Tính gold mặc định cho các cấp không phải milestone
     * Formula: level * 10 + 30 (min 40, max 100)
     */
    public static int getDefaultGold(int level) {
        if (level <= 1) return 0;
        int gold = level * 10 + 30;
        return Math.min(100, Math.max(40, gold));
    }

    /**
     * Kiểm tra có phải milestone không
     */
    public static boolean isMilestone(int level) {
        return findByLevel(level) != null;
    }
}
