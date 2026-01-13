package com.app.backend.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response cho tính năng chuỗi đăng nhập
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginStreakResponse {

    @JsonProperty("streak_hien_tai")
    private int streakHienTai;           // Số ngày đăng nhập liên tục

    @JsonProperty("ngay_trong_chu_ky")
    private int ngayTrongChuKy;          // Ngày thứ mấy trong chu kỳ 7 ngày (1-7)

    @JsonProperty("da_diem_danh_hom_nay")
    private boolean daDiemDanhHomNay;    // Đã điểm danh hôm nay chưa

    @JsonProperty("ngay_dang_nhap_cuoi")
    private LocalDate ngayDangNhapCuoi;  // Ngày đăng nhập gần nhất

    @JsonProperty("phan_thuong_hom_nay")
    private RewardDetail phanThuongHomNay; // Phần thưởng có thể nhận hôm nay

    @JsonProperty("danh_sach_ngay")
    private List<DayReward> danhSachNgay; // 7 ngày trong chu kỳ

    @JsonProperty("thong_bao")
    private String thongBao;

    /**
     * Thông tin phần thưởng
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardDetail {
        @JsonProperty("gold")
        private int gold;

        @JsonProperty("xp")
        private int xp;

        @JsonProperty("vat_pham_ten")
        private String vatPhamTen;

        @JsonProperty("vat_pham_icon")
        private String vatPhamIcon;

        @JsonProperty("so_luong_vat_pham")
        private int soLuongVatPham;
    }

    /**
     * Thông tin 1 ngày trong chu kỳ
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayReward {
        @JsonProperty("ngay")
        private int ngay;                // 1-7

        @JsonProperty("gold")
        private int gold;

        @JsonProperty("xp")
        private int xp;

        @JsonProperty("co_vat_pham")
        private boolean coVatPham;

        @JsonProperty("mo_ta")
        private String moTa;

        @JsonProperty("da_nhan")
        private boolean daNhan;          // Đã nhận thưởng ngày này chưa

        @JsonProperty("la_hom_nay")
        private boolean laHomNay;        // Có phải hôm nay không

        @JsonProperty("co_the_nhan")
        private boolean coTheNhan;       // Có thể nhận được không
    }
}
