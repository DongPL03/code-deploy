package com.app.backend.models.enums;

import lombok.Getter;

/**
 * Định nghĩa tất cả nhiệm vụ trong game
 * Không cần lưu DB - tối ưu bộ nhớ
 */
@Getter
public enum MaNhiemVu {
    // ============ NHIỆM VỤ NGÀY ============
    NGAY_CHOI_1("Tham gia 1 trận đấu", LoaiNhiemVu.HANG_NGAY, 1, 20, 0, null, "🎮"),
    NGAY_CHOI_3("Tham gia 3 trận đấu", LoaiNhiemVu.HANG_NGAY, 3, 50, 0, null, "🎮"),
    NGAY_THANG_1("Thắng 1 trận đấu", LoaiNhiemVu.HANG_NGAY, 1, 40, 0, null, "🏆"),
    NGAY_DUNG_10("Trả lời đúng 10 câu hỏi", LoaiNhiemVu.HANG_NGAY, 10, 40, 0, null, "✅"),
    NGAY_DUNG_30("Trả lời đúng 30 câu hỏi", LoaiNhiemVu.HANG_NGAY, 30, 100, 0, null, "✅"),
    NGAY_COMBO_5("Đạt combo 5 trong 1 trận", LoaiNhiemVu.HANG_NGAY, 5, 60, 0, null, "🔥"),

    // ============ NHIỆM VỤ TUẦN ============
    TUAN_CHOI_10("Tham gia 10 trận đấu", LoaiNhiemVu.HANG_TUAN, 10, 100, 0, null, "🎮"),
    TUAN_CHOI_20("Tham gia 20 trận đấu", LoaiNhiemVu.HANG_TUAN, 20, 200, 0, null, "🎮"),
    TUAN_THANG_5("Thắng 5 trận đấu", LoaiNhiemVu.HANG_TUAN, 5, 125, 0, null, "🏆"),
    TUAN_THANG_10("Thắng 10 trận đấu", LoaiNhiemVu.HANG_TUAN, 10, 350, 50, null, "🏆"),
    TUAN_DUNG_100("Trả lời đúng 100 câu hỏi", LoaiNhiemVu.HANG_TUAN, 100, 350, 0, null, "✅"),
    TUAN_DUNG_200("Trả lời đúng 200 câu hỏi", LoaiNhiemVu.HANG_TUAN, 150, 700, 100, null, "✅"),
    TUAN_TOP3_3("Đạt Top 3 trong 3 trận", LoaiNhiemVu.HANG_TUAN, 3, 175, 0, null, "🥇"),
    TUAN_RANKED_THANG_3("Thắng 3 trận Ranked", LoaiNhiemVu.HANG_TUAN, 3, 250, 0, "GOI_Y_50_50", "⚔️");

    private final String moTa;
    private final LoaiNhiemVu loai;
    private final int mucTieu;        // Số lượng cần đạt
    private final int goldThuong;     // Gold thưởng
    private final int xpThuong;       // XP thưởng (bonus)
    private final String vatPhamLoai; // Loại vật phẩm thưởng (nullable)
    private final String icon;

    MaNhiemVu(String moTa, LoaiNhiemVu loai, int mucTieu, int goldThuong, int xpThuong, String vatPhamLoai, String icon) {
        this.moTa = moTa;
        this.loai = loai;
        this.mucTieu = mucTieu;
        this.goldThuong = goldThuong;
        this.xpThuong = xpThuong;
        this.vatPhamLoai = vatPhamLoai;
        this.icon = icon;
    }

    /**
     * Lấy tất cả nhiệm vụ theo loại
     */
    public static MaNhiemVu[] getByLoai(LoaiNhiemVu loai) {
        return java.util.Arrays.stream(values())
                .filter(nv -> nv.getLoai() == loai)
                .toArray(MaNhiemVu[]::new);
    }
}
