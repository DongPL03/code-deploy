package com.app.backend.models;

import com.app.backend.models.enums.LoaiTinNhan;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "tin_nhan")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TinNhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "tran_dau_id")
    private TranDau tranDau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "gui_boi_id", nullable = false)
    private NguoiDung guiBoi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "nhan_boi_id")
    private NguoiDung nhanBoi;

    @Column(name = "noi_dung")
    private String noiDung;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_tin_nhan")
    @Builder.Default
    private LoaiTinNhan loaiTinNhan = LoaiTinNhan.VAN_BAN;

    @Column(name = "url_truyen_thong")
    private String urlMedia;

    @Column(name = "ten_file")
    private String tenFile;

    @Column(name = "kich_thuoc_file")
    private Long kichThuocFile;

    @Column(name = "gui_luc", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant guiLuc;

    @PrePersist
    protected void onCreate() {
        guiLuc = Instant.now();
    }
}