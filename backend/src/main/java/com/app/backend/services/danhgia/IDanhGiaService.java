package com.app.backend.services.danhgia;

import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.models.DanhGia;
import com.app.backend.models.enums.LoaiDoiTuongDanhGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

public interface IDanhGiaService {

    /**
     * Tạo hoặc cập nhật đánh giá
     */
    DanhGia createOrUpdateDanhGia(Long nguoiDungId, LoaiDoiTuongDanhGia loaiDoiTuong,
                                         Long doiTuongId, Integer soSao, String noiDung) throws DataNotFoundException;

    /**
     * Lấy đánh giá của user cho một đối tượng
     */
    Optional<DanhGia> getMyDanhGia(Long nguoiDungId, LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId);

    /**
     * Lấy danh sách đánh giá cho một đối tượng (phân trang)
     */
    Page<DanhGia> getDanhGiaByDoiTuong(LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId, Pageable pageable);

    /**
     * Lấy thống kê đánh giá cho một đối tượng
     */
    Map<String, Object> getStatsForDoiTuong(LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId);

    /**
     * Xóa đánh giá (soft delete)
     */
    void deleteDanhGia(Long danhGiaId, Long nguoiDungId) throws DataNotFoundException;
}


