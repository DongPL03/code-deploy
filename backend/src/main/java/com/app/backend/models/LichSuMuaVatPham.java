package com.app.backend.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu lịch sử mua vật phẩm từ Shop
 */
@Entity
@Table(name = "lich_su_mua_vat_pham")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuMuaVatPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    @JsonProperty("nguoi_dung")
    private NguoiDung nguoiDung;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vat_pham_id", nullable = false)
    @JsonProperty("vat_pham")
    private VatPham vatPham;

    @Column(name = "so_luong", nullable = false)
    @JsonProperty("so_luong")
    private Integer soLuong;

    @Column(name = "gia_mua", nullable = false)
    @JsonProperty("gia_mua")
    private Integer giaMua;

    @Column(name = "tong_gia", nullable = false)
    @JsonProperty("tong_gia")
    private Integer tongGia;

    @Column(name = "mua_luc", nullable = false)
    @JsonProperty("mua_luc")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime muaLuc;

    @PrePersist
    protected void onCreate() {
        if (muaLuc == null) {
            muaLuc = LocalDateTime.now();
        }
    }
}
