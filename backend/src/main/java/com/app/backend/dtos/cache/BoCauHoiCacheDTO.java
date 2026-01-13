package com.app.backend.dtos.cache;

import com.app.backend.models.BoCauHoi;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO dùng để cache bộ câu hỏi vào Redis
 * 
 * Lưu ý:
 * - Chỉ chứa dữ liệu tĩnh, không phụ thuộc vào user hiện tại
 * - Không chứa lazy-loaded relationships
 * - Implements Serializable để Jackson có thể serialize/deserialize
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoCauHoiCacheDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private Long id;
    private String tieuDe;
    private String moTa;
    private String cheDoHienThi;
    private String trangThai;
    private String loaiSuDung;
    private String lyDoTuChoi;
    
    // Thông tin chủ đề (flatten từ entity)
    private Long chuDeId;
    private String chuDeTen;
    
    // Thông tin người tạo (flatten từ entity)
    private Long nguoiTaoId;
    private String nguoiTaoHoTen;
    private String nguoiTaoVaiTro; // Để check admin
    
    // Thông tin mở khóa
    private Boolean canMoKhoa;
    private Long giaMoKhoa;
    private Boolean muonTaoTraPhi;
    
    // Thống kê
    private Integer soCauHoi;
    private Integer tongDanhGia;
    private Double trungBinhSao;
    
    // Timestamps
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant taoLuc;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant capNhatLuc;

    /**
     * Convert từ Entity sang Cache DTO
     * Gọi trong transaction để access lazy fields
     */
    public static BoCauHoiCacheDTO fromEntity(BoCauHoi entity) {
        if (entity == null) return null;
        
        return BoCauHoiCacheDTO.builder()
                .id(entity.getId())
                .tieuDe(entity.getTieuDe())
                .moTa(entity.getMoTa())
                .cheDoHienThi(entity.getCheDoHienThi())
                .trangThai(entity.getTrangThai())
                .loaiSuDung(entity.getLoaiSuDung())
                .lyDoTuChoi(entity.getLyDoTuChoi())
                // Chủ đề
                .chuDeId(entity.getChuDe() != null ? entity.getChuDe().getId() : null)
                .chuDeTen(entity.getChuDe() != null ? entity.getChuDe().getTen() : null)
                // Người tạo
                .nguoiTaoId(entity.getTaoBoi() != null ? entity.getTaoBoi().getId() : null)
                .nguoiTaoHoTen(entity.getTaoBoi() != null ? entity.getTaoBoi().getHoTen() : null)
                .nguoiTaoVaiTro(entity.getTaoBoi() != null && entity.getTaoBoi().getVaiTro() != null 
                        ? entity.getTaoBoi().getVaiTro().getTenVaiTro() : null)
                // Mở khóa
                .canMoKhoa(entity.getCanMoKhoa())
                .giaMoKhoa(entity.getGiaMoKhoa())
                .muonTaoTraPhi(entity.getMuonTaoTraPhi())
                // Thống kê
                .soCauHoi(entity.getSoCauHoi())
                .tongDanhGia(entity.getTongDanhGia())
                .trungBinhSao(entity.getTrungBinhSao())
                // Timestamps
                .taoLuc(entity.getTaoLuc())
                .capNhatLuc(entity.getCapNhatLuc())
                .build();
    }
}
