package com.app.backend.repositories;

import com.app.backend.models.DanhGia;
import com.app.backend.models.enums.LoaiDoiTuongDanhGia;
import com.app.backend.models.enums.TrangThaiDanhGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IDanhGiaRepository extends JpaRepository<DanhGia, Long> {

    /**
     * Tìm đánh giá của một user cho một đối tượng cụ thể
     */
    Optional<DanhGia> findByNguoiDungIdAndLoaiDoiTuongAndDoiTuongId(
            Long nguoiDungId, LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId);

    /**
     * Lấy danh sách đánh giá cho một đối tượng (phân trang)
     */
    Page<DanhGia> findByLoaiDoiTuongAndDoiTuongIdAndTrangThai(
            LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId,
            TrangThaiDanhGia trangThai, Pageable pageable);

    /**
     * Lấy tất cả đánh giá active cho một đối tượng
     */
    List<DanhGia> findByLoaiDoiTuongAndDoiTuongIdAndTrangThai(
            LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId, TrangThaiDanhGia trangThai);

    /**
     * Đếm số lượng đánh giá active của một đối tượng
     */
    long countByLoaiDoiTuongAndDoiTuongIdAndTrangThai(
            LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId, TrangThaiDanhGia trangThai);

    /**
     * Tính trung bình số sao của một đối tượng
     */
    @Query("SELECT AVG(d.soSao) FROM DanhGia d WHERE d.loaiDoiTuong = :loai AND d.doiTuongId = :doiTuongId AND d.trangThai = :trangThai")
    Double calculateAverageRating(@Param("loai") LoaiDoiTuongDanhGia loai,
                                  @Param("doiTuongId") Long doiTuongId,
                                  @Param("trangThai") TrangThaiDanhGia trangThai);

    /**
     * Đếm số lượng đánh giá theo từng mức sao
     */
    @Query("SELECT d.soSao, COUNT(d) FROM DanhGia d WHERE d.loaiDoiTuong = :loai AND d.doiTuongId = :doiTuongId AND d.trangThai = :trangThai GROUP BY d.soSao ORDER BY d.soSao DESC")
    List<Object[]> countBySoSao(@Param("loai") LoaiDoiTuongDanhGia loai,
                                @Param("doiTuongId") Long doiTuongId,
                                @Param("trangThai") TrangThaiDanhGia trangThai);

    /**
     * Lấy danh sách đánh giá mới nhất của user
     */
    Page<DanhGia> findByNguoiDungIdAndTrangThaiOrderByCapNhatLucDesc(
            Long nguoiDungId, TrangThaiDanhGia trangThai, Pageable pageable);

    /**
     * Đếm số đánh giá theo loại đối tượng
     */
    long countByLoaiDoiTuongAndTrangThai(LoaiDoiTuongDanhGia loaiDoiTuong, TrangThaiDanhGia trangThai);

    /**
     * Tính trung bình sao theo loại đối tượng
     */
    @Query("SELECT AVG(d.soSao) FROM DanhGia d WHERE d.loaiDoiTuong = :loai AND d.trangThai = :trangThai")
    Double calculateAverageByLoaiDoiTuong(@Param("loai") LoaiDoiTuongDanhGia loai,
                                          @Param("trangThai") TrangThaiDanhGia trangThai);
}
