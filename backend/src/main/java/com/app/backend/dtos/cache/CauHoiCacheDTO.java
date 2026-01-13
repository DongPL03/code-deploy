package com.app.backend.dtos.cache;

import com.app.backend.models.CauHoi;
import lombok.*;

import java.io.Serializable;

/**
 * DTO dùng để cache câu hỏi vào Redis
 * 
 * Không chứa lazy-loaded relationships (như BoCauHoi)
 * Chỉ chứa các fields cần thiết để hiển thị/xử lý
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CauHoiCacheDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private Long id;
    
    /**
     * ID bộ câu hỏi (flatten từ relationship)
     */
    private Long boCauHoiId;
    
    /**
     * Độ khó: DE, TRUNG_BINH, KHO
     */
    private String doKho;
    
    /**
     * Nội dung câu hỏi
     */
    private String noiDung;
    
    /**
     * Loại nội dung: VAN_BAN, HINH_ANH, AM_THANH
     */
    private String loaiNoiDung;
    
    /**
     * Đường dẫn file (nếu có hình/audio)
     */
    private String duongDanTep;
    
    /**
     * 4 lựa chọn
     */
    private String luaChonA;
    private String luaChonB;
    private String luaChonC;
    private String luaChonD;
    
    /**
     * Đáp án đúng: A, B, C, D
     */
    private Character dapAnDung;
    
    /**
     * Giải thích đáp án
     */
    private String giaiThich;

    /**
     * Convert từ Entity sang Cache DTO
     */
    public static CauHoiCacheDTO fromEntity(CauHoi entity) {
        if (entity == null) return null;
        
        return CauHoiCacheDTO.builder()
                .id(entity.getId())
                .boCauHoiId(entity.getBoCauHoi() != null ? entity.getBoCauHoi().getId() : null)
                .doKho(entity.getDoKho())
                .noiDung(entity.getNoiDung())
                .loaiNoiDung(entity.getLoaiNoiDung())
                .duongDanTep(entity.getDuongDanTep())
                .luaChonA(entity.getLuaChonA())
                .luaChonB(entity.getLuaChonB())
                .luaChonC(entity.getLuaChonC())
                .luaChonD(entity.getLuaChonD())
                .dapAnDung(entity.getDapAnDung())
                .giaiThich(entity.getGiaiThich())
                .build();
    }
}
