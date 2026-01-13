package com.app.backend.dtos.danhgia;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDanhGiaDTO {

    @NotBlank(message = "Loại đối tượng không được để trống")
    @JsonProperty("loai_doi_tuong")
    private String loaiDoiTuong; // BO_CAU_HOI, KHOA_HOC

    @NotNull(message = "ID đối tượng không được để trống")
    @JsonProperty("doi_tuong_id")
    private Long doiTuongId;

    @NotNull(message = "Số sao không được để trống")
    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    @JsonProperty("so_sao")
    private Integer soSao;

    @JsonProperty("noi_dung")
    private String noiDung;
}
