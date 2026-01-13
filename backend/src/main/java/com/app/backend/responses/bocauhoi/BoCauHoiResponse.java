package com.app.backend.responses.bocauhoi;

import com.app.backend.dtos.cache.BoCauHoiCacheDTO;
import com.app.backend.models.BoCauHoi;
import com.app.backend.models.NguoiDung;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoCauHoiResponse {

    private Long id;

    @JsonProperty("tieu_de")
    private String tieuDe;

    @JsonProperty("mo_ta")
    private String moTa;

    @JsonProperty("che_do_hien_thi")
    private String cheDoHienThi;

    @JsonProperty("trang_thai")
    private String trangThai;

    @JsonProperty("loai_su_dung")
    private String loaiSuDung;

    @JsonProperty("ly_do_tu_choi")
    private String lyDoTuChoi;

    @JsonProperty("chu_de")
    private String chuDe;

    @JsonProperty("chu_de_id")
    private Long chuDeId;

    @JsonProperty("nguoi_tao")
    private String nguoiTao;

    @JsonProperty("nguoi_tao_id")
    private Long nguoiTaoId;

    @JsonProperty("co_quyen_sua")
    private Boolean coQuyenSua;

    @JsonProperty("gia_mo_khoa")
    private Long giaMoKhoa;

    @JsonProperty("da_mo_khoa")
    private Boolean daMoKhoa;

    @JsonProperty("can_mo_khoa")
    private Boolean canMoKhoa;

    /**
     * User muốn tạo bộ câu hỏi trả phí hay không
     * true = muốn tạo trả phí (admin sẽ set giá khi duyệt)
     * false = muốn tạo miễn phí
     */
    @JsonProperty("muon_tao_tra_phi")
    private Boolean muonTaoTraPhi;

    // Thông tin liên kết khóa học (nếu bộ câu hỏi thuộc một khóa học nào đó)
    @JsonProperty("thuoc_khoa_hoc")
    private Boolean thuocKhoaHoc;

    @JsonProperty("khoa_hoc_id")
    private Long khoaHocId;

    @JsonProperty("khoa_hoc_ten")
    private String khoaHocTen;

    /**
     * Số lượng câu hỏi trong bộ câu hỏi
     */
    @JsonProperty("so_cau_hoi")
    private Integer soCauHoi;

    /**
     * Tổng số đánh giá
     */
    @JsonProperty("tong_danh_gia")
    private Integer tongDanhGia;

    /**
     * Điểm trung bình sao (1-5)
     */
    @JsonProperty("trung_binh_sao")
    private Double trungBinhSao;

    @JsonProperty("tao_luc")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant taoLuc;

    @JsonProperty("cap_nhat_luc")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant capNhatLuc;

    // ✅ static mapper
    public static BoCauHoiResponse from(BoCauHoi entity) {
        NguoiDung taoBoi = entity.getTaoBoi();
        boolean la_admin_tao = taoBoi != null && taoBoi.getVaiTro() != null && "admin".equals(taoBoi.getVaiTro().getTenVaiTro());
        return BoCauHoiResponse.builder()
                .id(entity.getId())
                .tieuDe(entity.getTieuDe())
                .moTa(entity.getMoTa())
                .cheDoHienThi(entity.getCheDoHienThi() != null ? entity.getCheDoHienThi() : null)
                .trangThai(entity.getTrangThai())
                .loaiSuDung(entity.getLoaiSuDung())
                .lyDoTuChoi(entity.getLyDoTuChoi())
                .chuDe(entity.getChuDe() != null ? entity.getChuDe().getTen() : null)
                .chuDeId(entity.getChuDe() != null ? entity.getChuDe().getId() : null)
                .nguoiTao(taoBoi != null ? taoBoi.getHoTen() : null)
                .nguoiTaoId(taoBoi != null ? taoBoi.getId() : null)
                .coQuyenSua(la_admin_tao)
                // Các field mở khoá cũng cần trả về cho danh sách chung
                .canMoKhoa(entity.getCanMoKhoa())
                .thuocKhoaHoc(false)
                .giaMoKhoa(entity.getGiaMoKhoa())
                .muonTaoTraPhi(entity.getMuonTaoTraPhi())
                .soCauHoi(entity.getSoCauHoi())
                .tongDanhGia(entity.getTongDanhGia())
                .trungBinhSao(entity.getTrungBinhSao())
                // Ở danh sách chung chưa có thông tin theo user, mặc định false
                .daMoKhoa(false)
                .taoLuc(entity.getTaoLuc())
                .capNhatLuc(entity.getCapNhatLuc())
                .build();
    }

    public static BoCauHoiResponse from(BoCauHoi entity, boolean daMoKhoa) {
        NguoiDung taoBoi = entity.getTaoBoi();
        boolean la_admin_tao = taoBoi != null && taoBoi.getVaiTro() != null && "admin".equals(taoBoi.getVaiTro().getTenVaiTro());
        return BoCauHoiResponse.builder()
                .id(entity.getId())
                .tieuDe(entity.getTieuDe())
                .moTa(entity.getMoTa())
                .cheDoHienThi(entity.getCheDoHienThi() != null ? entity.getCheDoHienThi() : null)
                .trangThai(entity.getTrangThai())
                .loaiSuDung(entity.getLoaiSuDung())
                .lyDoTuChoi(entity.getLyDoTuChoi())
                .chuDe(entity.getChuDe() != null ? entity.getChuDe().getTen() : null)
                .chuDeId(entity.getChuDe() != null ? entity.getChuDe().getId() : null)
                .nguoiTao(taoBoi != null ? taoBoi.getHoTen() : null)
                .nguoiTaoId(taoBoi != null ? taoBoi.getId() : null)
                .coQuyenSua(la_admin_tao)
                .taoLuc(entity.getTaoLuc())
                .canMoKhoa(entity.getCanMoKhoa())
                .giaMoKhoa(entity.getGiaMoKhoa())
                .muonTaoTraPhi(entity.getMuonTaoTraPhi())
                .soCauHoi(entity.getSoCauHoi())
                .tongDanhGia(entity.getTongDanhGia())
                .trungBinhSao(entity.getTrungBinhSao())
                .daMoKhoa(daMoKhoa)
                .thuocKhoaHoc(false)
                .capNhatLuc(entity.getCapNhatLuc())
                .build();
    }

    /**
     * Convert từ Cache DTO sang Response
     * Dùng khi lấy data từ Redis cache
     * 
     * @param cache Cache DTO
     * @param daMoKhoa Flag mở khóa (phụ thuộc vào user hiện tại - không cache)
     */
    public static BoCauHoiResponse fromCache(BoCauHoiCacheDTO cache, boolean daMoKhoa) {
        if (cache == null) return null;
        
        boolean laAdminTao = "admin".equals(cache.getNguoiTaoVaiTro());
        
        return BoCauHoiResponse.builder()
                .id(cache.getId())
                .tieuDe(cache.getTieuDe())
                .moTa(cache.getMoTa())
                .cheDoHienThi(cache.getCheDoHienThi())
                .trangThai(cache.getTrangThai())
                .loaiSuDung(cache.getLoaiSuDung())
                .lyDoTuChoi(cache.getLyDoTuChoi())
                .chuDe(cache.getChuDeTen())
                .chuDeId(cache.getChuDeId())
                .nguoiTao(cache.getNguoiTaoHoTen())
                .nguoiTaoId(cache.getNguoiTaoId())
                .coQuyenSua(laAdminTao)
                .taoLuc(cache.getTaoLuc())
                .canMoKhoa(cache.getCanMoKhoa())
                .giaMoKhoa(cache.getGiaMoKhoa())
                .muonTaoTraPhi(cache.getMuonTaoTraPhi())
                .soCauHoi(cache.getSoCauHoi())
                .tongDanhGia(cache.getTongDanhGia())
                .trungBinhSao(cache.getTrungBinhSao())
                .daMoKhoa(daMoKhoa)
                .thuocKhoaHoc(false)
                .capNhatLuc(cache.getCapNhatLuc())
                .build();
    }
}
