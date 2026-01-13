package com.app.backend.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Response khi người chơi lên cấp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelUpResponse {

    /**
     * Có lên cấp không
     */
    @JsonProperty("da_len_cap")
    private boolean daLenCap;

    /**
     * Cấp độ trước đó
     */
    @JsonProperty("cap_do_cu")
    private Integer capDoCu;

    /**
     * Cấp độ mới
     */
    @JsonProperty("cap_do_moi")
    private Integer capDoMoi;

    /**
     * XP hiện tại trong cấp
     */
    @JsonProperty("xp_hien_tai")
    private Long xpHienTai;

    /**
     * XP cần để lên cấp tiếp theo
     */
    @JsonProperty("xp_can_len_cap")
    private Long xpCanLenCap;

    /**
     * Phần trăm tiến độ
     */
    @JsonProperty("phan_tram_tien_do")
    private Double phanTramTienDo;

    /**
     * Danh sách phần thưởng nhận được (nếu lên cấp)
     */
    @JsonProperty("phan_thuong")
    private List<RewardItem> phanThuong;

    /**
     * Thông báo
     */
    @JsonProperty("thong_bao")
    private String thongBao;

    /**
     * Chi tiết từng phần thưởng
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RewardItem {
        @JsonProperty("loai")
        private String loai; // "GOLD", "VAT_PHAM", "XP"

        @JsonProperty("ten")
        private String ten;

        @JsonProperty("so_luong")
        private Integer soLuong;

        @JsonProperty("icon")
        private String icon;

        @JsonProperty("cap_do")
        private Integer capDo; // Cấp độ nhận thưởng này
    }
}
