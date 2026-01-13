package com.app.backend.models.enums;

import lombok.Getter;

/**
 * Định nghĩa phần thưởng chuỗi đăng nhập - FIX CỨNG trong code
 * Không cần lưu DB - tối ưu hiệu suất
 * 
 * Thiết kế:
 * - Ngày 1-6: Gold tăng dần
 * - Ngày 7: Thưởng lớn + vật phẩm
 * - Sau ngày 7: reset về ngày 1
 */
@Getter
public enum PhanThuongDangNhap {
    DAY_1(1, 20, 0, null, 0, "🌟 Ngày 1"),
    DAY_2(2, 30, 0, null, 0, "🌟 Ngày 2"),
    DAY_3(3, 50, 0, null, 0, "🌟 Ngày 3"),
    DAY_4(4, 70, 0, null, 0, "🌟 Ngày 4"),
    DAY_5(5, 100, 0, null, 0, "🌟 Ngày 5"),
    DAY_6(6, 150, 0, null, 0, "🌟 Ngày 6"),
    DAY_7(7, 300, 50, LoaiVatPham.GOI_Y_50_50, 2, "🎁 Ngày 7 - Thưởng lớn!");

    private final int ngay;           // Ngày thứ mấy trong chuỗi
    private final int goldThuong;     // Gold thưởng
    private final int xpThuong;       // XP thưởng (bonus)
    private final LoaiVatPham vatPhamLoai; // Vật phẩm thưởng (nullable)
    private final int soLuongVatPham; // Số lượng vật phẩm
    private final String moTa;        // Mô tả hiển thị

    PhanThuongDangNhap(int ngay, int goldThuong, int xpThuong, LoaiVatPham vatPhamLoai, int soLuongVatPham, String moTa) {
        this.ngay = ngay;
        this.goldThuong = goldThuong;
        this.xpThuong = xpThuong;
        this.vatPhamLoai = vatPhamLoai;
        this.soLuongVatPham = soLuongVatPham;
        this.moTa = moTa;
    }

    /**
     * Lấy phần thưởng theo ngày
     */
    public static PhanThuongDangNhap getByDay(int day) {
        // Nếu day > 7, reset về 1-7
        int normalizedDay = ((day - 1) % 7) + 1;
        for (PhanThuongDangNhap pt : values()) {
            if (pt.getNgay() == normalizedDay) {
                return pt;
            }
        }
        return DAY_1;
    }

    /**
     * Tổng gold có thể nhận trong 1 tuần
     */
    public static int getTotalWeeklyGold() {
        int total = 0;
        for (PhanThuongDangNhap pt : values()) {
            total += pt.getGoldThuong();
        }
        return total; // = 720 gold/tuần
    }

    /**
     * Số ngày tối đa trong 1 chu kỳ
     */
    public static final int MAX_STREAK_CYCLE = 7;
}
