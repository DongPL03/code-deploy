package com.app.backend.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "luot_thich_binh_luan", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"binh_luan_id", "nguoi_dung_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuotThichBinhLuan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "binh_luan_id", nullable = false)
    private BinhLuan binhLuan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

    @Column(name = "tao_luc")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant taoLuc;

    @PrePersist
    protected void onCreate() {
        taoLuc = Instant.now();
    }
}
