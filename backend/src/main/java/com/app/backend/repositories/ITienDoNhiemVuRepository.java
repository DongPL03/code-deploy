package com.app.backend.repositories;

import com.app.backend.models.TienDoNhiemVu;
import com.app.backend.models.enums.LoaiNhiemVu;
import com.app.backend.models.enums.MaNhiemVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ITienDoNhiemVuRepository extends JpaRepository<TienDoNhiemVu, Long> {

    /**
     * Tìm tiến độ nhiệm vụ cụ thể của user trong chu kỳ
     */
    Optional<TienDoNhiemVu> findByNguoiDung_IdAndMaNhiemVuAndNgayBatDau(
            Long nguoiDungId, MaNhiemVu maNhiemVu, LocalDate ngayBatDau);

    /**
     * Lấy tất cả tiến độ nhiệm vụ của user trong chu kỳ
     */
    List<TienDoNhiemVu> findAllByNguoiDung_IdAndNgayBatDau(Long nguoiDungId, LocalDate ngayBatDau);

    /**
     * Lấy nhiệm vụ theo loại (DAILY/WEEKLY) trong chu kỳ
     */
    @Query("SELECT t FROM TienDoNhiemVu t WHERE t.nguoiDung.id = :userId " +
            "AND t.maNhiemVu IN :maNhiemVus AND t.ngayBatDau = :ngayBatDau")
    List<TienDoNhiemVu> findByUserAndMaNhiemVusAndNgay(
            @Param("userId") Long userId,
            @Param("maNhiemVus") List<MaNhiemVu> maNhiemVus,
            @Param("ngayBatDau") LocalDate ngayBatDau);

    /**
     * Đếm số nhiệm vụ đã hoàn thành trong chu kỳ
     */
    @Query("SELECT COUNT(t) FROM TienDoNhiemVu t WHERE t.nguoiDung.id = :userId " +
            "AND t.ngayBatDau = :ngayBatDau AND t.daHoanThanh = true")
    long countCompletedByUserAndDate(@Param("userId") Long userId, @Param("ngayBatDau") LocalDate ngayBatDau);

    /**
     * Lấy các nhiệm vụ chưa nhận thưởng
     */
    List<TienDoNhiemVu> findByNguoiDung_IdAndDaHoanThanhTrueAndDaNhanThuongFalse(Long nguoiDungId);

    /**
     * Xóa tiến độ cũ (cleanup job)
     */
    void deleteByNgayBatDauBefore(LocalDate date);
}
