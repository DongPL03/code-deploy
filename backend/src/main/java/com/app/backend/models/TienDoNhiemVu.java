package com.app.backend.models;

import com.app.backend.models.enums.MaNhiemVu;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Lưu tiến độ nhiệm vụ của người dùng
 * Reset theo chu kỳ (ngày/tuần)
 */
@Entity
@Table(name = "tien_do_nhiem_vu",
        uniqueConstraints = @UniqueConstraint(columnNames = {"nguoi_dung_id", "ma_nhiem_vu", "ngay_bat_dau"}),
        indexes = {
                @Index(name = "idx_tien_do_nguoi_dung", columnList = "nguoi_dung_id"),
                @Index(name = "idx_tien_do_ngay", columnList = "ngay_bat_dau")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TienDoNhiemVu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

    /**
     * Mã nhiệm vụ (từ enum MaNhiemVu)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ma_nhiem_vu", nullable = false, length = 50)
    private MaNhiemVu maNhiemVu;

    /**
     * Tiến độ hiện tại
     */
    @Column(name = "tien_do", nullable = false)
    @Builder.Default
    private Integer tienDo = 0;

    /**
     * Đã hoàn thành chưa
     */
    @Column(name = "da_hoan_thanh", nullable = false)
    @Builder.Default
    private Boolean daHoanThanh = false;

    /**
     * Đã nhận thưởng chưa
     */
    @Column(name = "da_nhan_thuong", nullable = false)
    @Builder.Default
    private Boolean daNhanThuong = false;

    /**
     * Ngày bắt đầu chu kỳ (dùng để reset)
     * - DAILY: Ngày hiện tại
     * - WEEKLY: Ngày thứ 2 của tuần
     */
    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDate ngayBatDau;

    /**
     * Tăng tiến độ và kiểm tra hoàn thành
     */
    public void tangTienDo(int amount) {
        this.tienDo = Math.min(this.tienDo + amount, this.maNhiemVu.getMucTieu());
        if (this.tienDo >= this.maNhiemVu.getMucTieu()) {
            this.daHoanThanh = true;
        }
    }
}
