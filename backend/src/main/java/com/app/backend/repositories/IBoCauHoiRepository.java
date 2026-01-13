package com.app.backend.repositories;

import com.app.backend.models.BoCauHoi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IBoCauHoiRepository extends JpaRepository<BoCauHoi, Long> {
    @Query("""
            SELECT b FROM BoCauHoi b
            WHERE b.isXoa = false
            AND (:keyword IS NULL OR :keyword = '' OR LOWER(b.tieuDe) LIKE %:keyword%)
            AND (:chuDeId IS NULL OR :chuDeId = 0 OR b.chuDe.id = :chuDeId)
            AND (:cheDoHienThi IS NULL OR :cheDoHienThi = '' OR b.cheDoHienThi = :cheDoHienThi)
            AND (:trangThai IS NULL OR :trangThai = '' OR b.trangThai = :trangThai)
            AND (:loaiSuDung IS NULL OR :loaiSuDung = '' OR b.loaiSuDung = :loaiSuDung)
            AND (:muonTaoTraPhi IS NULL OR b.muonTaoTraPhi = :muonTaoTraPhi)
            AND (:nguoiTaoId IS NULL OR :nguoiTaoId = 0 OR b.taoBoi.id = :nguoiTaoId)
            AND (:minRating IS NULL OR b.trungBinhSao >= :minRating)
            AND (:maxRating IS NULL OR b.trungBinhSao <= :maxRating)
            AND (
                 :isAdmin = true
                 OR b.taoBoi.id = :creatorId
                 OR (b.cheDoHienThi = 'CONG_KHAI' AND b.trangThai = 'DA_DUYET')
            )
            """)
    Page<BoCauHoi> searchBoCauHoi(Pageable pageable,
                                  @Param("keyword") String keyword,
                                  @Param("chuDeId") Long chuDeId,
                                  @Param("cheDoHienThi") String cheDoHienThi,
                                  @Param("trangThai") String trangThai,
                                  @Param("loaiSuDung") String loaiSuDung,
                                  @Param("muonTaoTraPhi") Boolean muonTaoTraPhi,
                                  @Param("nguoiTaoId") Long nguoiTaoId,
                                  @Param("minRating") Double minRating,
                                  @Param("maxRating") Double maxRating,
                                  @Param("creatorId") Long creatorId,
                                  @Param("isAdmin") boolean isAdmin);

    /**
     * Tìm bộ câu hỏi dành cho practice free (không thuộc khóa học)
     * Loại trừ các bộ câu hỏi thuộc khóa học (check qua khoa_hoc_bo_cau_hoi)
     */
    @Query("""
            SELECT DISTINCT b FROM BoCauHoi b
            WHERE b.isXoa = false
              AND b.trangThai = 'DA_DUYET'
              AND (b.loaiSuDung = 'CHI_LUYEN_TAP')
              AND NOT EXISTS (
                  SELECT 1 FROM com.app.backend.models.KhoaHocBoCauHoi khbch 
                  WHERE khbch.boCauHoi.id = b.id
              )
              AND (
                    :isAdmin = true
                    OR b.taoBoi.id = :creatorId
                    OR b.cheDoHienThi = 'CONG_KHAI'
              )
            """)
    Page<BoCauHoi> findPracticeSets(Pageable pageable,
                                    @Param("creatorId") Long creatorId,
                                    @Param("isAdmin") boolean isAdmin);
    
    /**
     * Tìm bộ câu hỏi thuộc một khóa học cụ thể
     */
    @Query("""
            SELECT b FROM BoCauHoi b
            INNER JOIN com.app.backend.models.KhoaHocBoCauHoi khbch ON khbch.boCauHoi.id = b.id
            WHERE b.isXoa = false
              AND b.trangThai = 'DA_DUYET'
              AND khbch.khoaHoc.id = :khoaHocId
            ORDER BY khbch.thuTu ASC
            """)
    List<BoCauHoi> findBoCauHoiByKhoaHocId(@Param("khoaHocId") Long khoaHocId);
    
    /**
     * Kiểm tra xem bộ câu hỏi có thuộc khóa học nào không
     */
    @Query("""
            SELECT COUNT(khbch) > 0 FROM com.app.backend.models.KhoaHocBoCauHoi khbch
            WHERE khbch.boCauHoi.id = :boCauHoiId
            """)
    boolean isBelongToKhoaHoc(@Param("boCauHoiId") Long boCauHoiId);

    @Query("""
            SELECT b FROM BoCauHoi b
            WHERE b.isXoa = false
              AND b.trangThai = 'DA_DUYET'
              AND b.cheDoHienThi = 'RIENG_TU'
            """)
    Page<BoCauHoi> findBattleSets(Pageable pageable);

    @Query("""
            SELECT b FROM BoCauHoi b
            WHERE b.isXoa = false
              AND b.trangThai = 'DA_DUYET'
              AND (b.loaiSuDung = 'CHI_XEP_HANG')
            """)
    List<BoCauHoi> findRankedBattleSets();

    /**
     * Tìm bộ câu hỏi dành cho Casual Battle (đấu vui)
     * - Chỉ lấy CASUAL_ONLY hoặc BOTH (không lấy PRACTICE_ONLY hoặc RANKED_ONLY)
     * - Loại trừ các bộ thuộc khóa học (check qua khoa_hoc_bo_cau_hoi)
     * - Loại trừ các bộ chỉ dành cho practice (PRACTICE_ONLY)
     * - Loại trừ các bộ chỉ dành cho ranked (RANKED_ONLY)
     * - Cho phép cả official và non-official sets
     */
    @Query("""
            SELECT DISTINCT b FROM BoCauHoi b
            WHERE b.isXoa = false
              AND b.trangThai = 'DA_DUYET'
              AND (b.loaiSuDung = 'CHI_THUONG')
              AND NOT EXISTS (
                  SELECT 1 FROM com.app.backend.models.KhoaHocBoCauHoi khbch
                  WHERE khbch.boCauHoi.id = b.id
              )
              AND (
                    :isAdmin = true
                    or b.taoBoi.vaiTro.tenVaiTro = "admin"
                    OR b.taoBoi.id = :userId
                    OR b.cheDoHienThi = 'RIENG_TU'
              )
            """)
    List<BoCauHoi> findCasualBattleSets(@Param("userId") Long userId, @Param("isAdmin") boolean isAdmin);


    @Override
    long count();

    long countByTrangThai(String trangThai);

    // ============= RATING STATISTICS =============
    
    /**
     * Top bộ câu hỏi có rating cao nhất
     */
    @Query("""
            SELECT b FROM BoCauHoi b
            WHERE b.isXoa = false
              AND b.trangThai = 'DA_DUYET'
              AND b.tongDanhGia > 0
            ORDER BY b.trungBinhSao DESC, b.tongDanhGia DESC
            """)
    List<BoCauHoi> findTopRated(Pageable pageable);

    /**
     * Top bộ câu hỏi có nhiều đánh giá nhất
     */
    @Query("""
            SELECT b FROM BoCauHoi b
            WHERE b.isXoa = false
              AND b.trangThai = 'DA_DUYET'
              AND b.tongDanhGia > 0
            ORDER BY b.tongDanhGia DESC, b.trungBinhSao DESC
            """)
    List<BoCauHoi> findMostReviewed(Pageable pageable);

    /**
     * Bộ câu hỏi có rating thấp nhất (cần cải thiện)
     */
    @Query("""
            SELECT b FROM BoCauHoi b
            WHERE b.isXoa = false
              AND b.trangThai = 'DA_DUYET'
              AND b.tongDanhGia > 0
            ORDER BY b.trungBinhSao ASC, b.tongDanhGia DESC
            """)
    List<BoCauHoi> findLowestRated(Pageable pageable);

}
