package com.app.backend.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * DTO để mua vật phẩm từ Shop
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MuaVatPhamDTO {

    /**
     * ID vật phẩm muốn mua
     */
    @JsonProperty("vat_pham_id")
    private Long vatPhamId;

    /**
     * Số lượng muốn mua
     */
    @JsonProperty("so_luong")
    private Integer soLuong = 1;
}
