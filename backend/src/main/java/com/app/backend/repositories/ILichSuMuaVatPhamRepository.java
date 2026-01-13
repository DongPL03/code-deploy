package com.app.backend.repositories;

import com.app.backend.models.LichSuMuaVatPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ILichSuMuaVatPhamRepository extends JpaRepository<LichSuMuaVatPham, Long> {

    /**
     * Lấy lịch sử mua hàng của user
     */
    List<LichSuMuaVatPham> findByNguoiDungIdOrderByMuaLucDesc(Long nguoiDungId);

    /**
     * Đếm số lượng vật phẩm đã mua trong khoảng thời gian
     * (dùng để giới hạn mua Legendary/Epic)
     */
    @Query("SELECT COALESCE(SUM(l.soLuong), 0) FROM LichSuMuaVatPham l " +
            "WHERE l.nguoiDung.id = :userId AND l.vatPham.id = :vatPhamId " +
            "AND l.muaLuc >= :startTime AND l.muaLuc < :endTime")
    int countPurchasesInPeriod(@Param("userId") Long userId,
                               @Param("vatPhamId") Long vatPhamId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * Đếm tổng số vật phẩm theo độ hiếm đã mua trong tuần
     */
    @Query("SELECT COALESCE(SUM(l.soLuong), 0) FROM LichSuMuaVatPham l " +
            "WHERE l.nguoiDung.id = :userId AND l.vatPham.doHiem = :doHiem " +
            "AND l.muaLuc >= :startTime AND l.muaLuc < :endTime")
    int countPurchasesByRarityInPeriod(@Param("userId") Long userId,
                                       @Param("doHiem") String doHiem,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
}
