package com.app.backend.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Response khi mua vật phẩm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MuaVatPhamResponse {

    /**
     * Mua thành công hay không
     */
    @JsonProperty("thanh_cong")
    private boolean thanhCong;

    /**
     * Thông báo kết quả
     */
    @JsonProperty("thong_bao")
    private String thongBao;

    /**
     * Tên vật phẩm đã mua
     */
    @JsonProperty("ten_vat_pham")
    private String tenVatPham;

    /**
     * Số lượng đã mua
     */
    @JsonProperty("so_luong")
    private Integer soLuong;

    /**
     * Tổng giá đã trả
     */
    @JsonProperty("tong_gia")
    private Integer tongGia;

    /**
     * Số xu còn lại sau khi mua
     */
    @JsonProperty("tien_vang_con_lai")
    private Long tienVangConLai;

    /**
     * Số lượng vật phẩm trong inventory sau khi mua
     */
    @JsonProperty("so_luong_trong_inventory")
    private Integer soLuongTrongInventory;
}
