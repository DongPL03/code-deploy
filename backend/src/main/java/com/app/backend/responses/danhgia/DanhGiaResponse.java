package com.app.backend.responses.danhgia;

import com.app.backend.models.DanhGia;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DanhGiaResponse {

    private Long id;

    @JsonProperty("nguoi_dung_id")
    private Long nguoiDungId;

    @JsonProperty("nguoi_dung_ten")
    private String nguoiDungTen;

    @JsonProperty("nguoi_dung_avatar")
    private String nguoiDungAvatar;

    @JsonProperty("loai_doi_tuong")
    private String loaiDoiTuong;

    @JsonProperty("doi_tuong_id")
    private Long doiTuongId;

    @JsonProperty("so_sao")
    private Integer soSao;

    @JsonProperty("noi_dung")
    private String noiDung;

    @JsonProperty("tao_luc")
    private Instant taoLuc;

    @JsonProperty("cap_nhat_luc")
    private Instant capNhatLuc;

    public static DanhGiaResponse fromEntity(DanhGia danhGia) {
        return DanhGiaResponse.builder()
                .id(danhGia.getId())
                .nguoiDungId(danhGia.getNguoiDung().getId())
                .nguoiDungTen(danhGia.getNguoiDung().getHoTen())
                .nguoiDungAvatar(danhGia.getNguoiDung().getAvatarUrl())
                .loaiDoiTuong(danhGia.getLoaiDoiTuong().name())
                .doiTuongId(danhGia.getDoiTuongId())
                .soSao(danhGia.getSoSao())
                .noiDung(danhGia.getNoiDung())
                .taoLuc(danhGia.getTaoLuc())
                .capNhatLuc(danhGia.getCapNhatLuc())
                .build();
    }
}
