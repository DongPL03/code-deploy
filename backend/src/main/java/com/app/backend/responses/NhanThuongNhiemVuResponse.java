package com.app.backend.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Response khi nhận thưởng nhiệm vụ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhanThuongNhiemVuResponse {

    @JsonProperty("thanh_cong")
    private boolean thanhCong;

    @JsonProperty("thong_bao")
    private String thongBao;

    @JsonProperty("phan_thuong")
    private List<RewardItem> phanThuong;

    @JsonProperty("gold_moi")
    private Long goldMoi;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RewardItem {
        @JsonProperty("loai")
        private String loai; // "GOLD", "XP", "VAT_PHAM"

        @JsonProperty("ten")
        private String ten;

        @JsonProperty("so_luong")
        private int soLuong;

        @JsonProperty("icon")
        private String icon;
    }
}
