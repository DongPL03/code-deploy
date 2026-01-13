package com.app.backend.responses.danhgia;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DanhGiaStatsResponse {

    @JsonProperty("tong_danh_gia")
    private Long tongDanhGia;

    @JsonProperty("trung_binh_sao")
    private Double trungBinhSao;

    @JsonProperty("phan_bo_sao")
    private Map<Integer, Long> phanBoSao;
}
