package com.app.backend.responses;

import com.app.backend.models.VatPham;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Response cho Shop vật phẩm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopResponse {

    /**
     * Danh sách vật phẩm trong shop
     */
    @JsonProperty("vat_pham_list")
    private List<ShopItemResponse> vatPhamList;

    /**
     * Số xu hiện tại của user
     */
    @JsonProperty("tien_vang_hien_tai")
    private Long tienVangHienTai;

    /**
     * Response cho từng vật phẩm trong shop
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShopItemResponse {
        private Long id;
        private String ten;

        @JsonProperty("mo_ta")
        private String moTa;

        private String loai;
        private String icon;

        @JsonProperty("mau_sac")
        private String mauSac;

        @JsonProperty("gia_xu")
        private Integer giaXu;

        @JsonProperty("do_hiem")
        private String doHiem;

        /**
         * User có đủ tiền mua không
         */
        @JsonProperty("co_the_mua")
        private Boolean coTheMua;

        /**
         * Số lượng còn có thể mua trong tuần (cho Epic/Legendary)
         */
        @JsonProperty("so_luong_con_lai_tuan")
        private Integer soLuongConLaiTuan;

        /**
         * Thông báo giới hạn (nếu có)
         */
        @JsonProperty("thong_bao_gioi_han")
        private String thongBaoGioiHan;

        public static ShopItemResponse fromVatPham(VatPham vp, Long userGold, Integer soLuongConLaiTuan, String thongBaoGioiHan) {
            return ShopItemResponse.builder()
                    .id(vp.getId())
                    .ten(vp.getTen())
                    .moTa(vp.getMoTa())
                    .loai(vp.getLoai().name())
                    .icon(vp.getIcon())
                    .mauSac(vp.getMauSac())
                    .giaXu(vp.getGiaXu())
                    .doHiem(vp.getDoHiem())
                    .coTheMua(userGold >= vp.getGiaXu() && soLuongConLaiTuan > 0)
                    .soLuongConLaiTuan(soLuongConLaiTuan)
                    .thongBaoGioiHan(thongBaoGioiHan)
                    .build();
        }
    }
}
