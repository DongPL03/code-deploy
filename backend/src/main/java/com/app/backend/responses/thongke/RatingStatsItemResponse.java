package com.app.backend.responses.thongke;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RatingStatsItemResponse {
    private Long id;

    @JsonProperty("tieu_de")
    private String tieuDe;

    @JsonProperty("chu_de")
    private String chuDe;

    @JsonProperty("nguoi_tao")
    private String nguoiTao;

    @JsonProperty("trung_binh_sao")
    private Double trungBinhSao;

    @JsonProperty("tong_danh_gia")
    private Integer tongDanhGia;

    @JsonProperty("loai")
    private String loai; // BO_CAU_HOI hoặc KHOA_HOC
}
