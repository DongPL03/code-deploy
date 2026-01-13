package com.app.backend.models;

import com.app.backend.models.enums.LoaiDoiTuongDanhGia;
import com.app.backend.models.enums.TrangThaiDanhGia;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "danh_gia", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"nguoi_dung_id", "loai_doi_tuong", "doi_tuong_id"})
})
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DanhGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_doi_tuong", nullable = false, length = 50)
    private LoaiDoiTuongDanhGia loaiDoiTuong;

    @Column(name = "doi_tuong_id", nullable = false)
    private Long doiTuongId;

    @Min(1)
    @Max(5)
    @Column(name = "so_sao", nullable = false)
    private Integer soSao;

    @Column(name = "noi_dung", columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "tao_luc")
    private Instant taoLuc;

    @Column(name = "cap_nhat_luc")
    private Instant capNhatLuc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", length = 20)
    @Builder.Default
    private TrangThaiDanhGia trangThai = TrangThaiDanhGia.HOAT_DONG;

    @PrePersist
    protected void onCreate() {
        taoLuc = Instant.now();
        capNhatLuc = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        capNhatLuc = Instant.now();
    }
}
