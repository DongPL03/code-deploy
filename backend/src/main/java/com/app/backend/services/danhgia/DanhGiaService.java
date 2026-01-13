package com.app.backend.services.danhgia;

import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.models.DanhGia;
import com.app.backend.models.NguoiDung;
import com.app.backend.models.enums.LoaiDoiTuongDanhGia;
import com.app.backend.models.enums.TrangThaiDanhGia;
import com.app.backend.repositories.IBoCauHoiRepository;
import com.app.backend.repositories.IKhoaHocRepository;
import com.app.backend.repositories.INguoiDungRepository;
import com.app.backend.repositories.IDanhGiaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DanhGiaService implements IDanhGiaService {

    private final IDanhGiaRepository danhGiaRepository;
    private final INguoiDungRepository nguoiDungRepository;
    private final IBoCauHoiRepository boCauHoiRepository;
    private final IKhoaHocRepository khoaHocRepository;

    /**
     * Tạo hoặc cập nhật đánh giá
     */
    @Override
    @Transactional
    public DanhGia createOrUpdateDanhGia(Long nguoiDungId, LoaiDoiTuongDanhGia loaiDoiTuong,
                                         Long doiTuongId, Integer soSao, String noiDung) throws DataNotFoundException {
        // Validate user
        NguoiDung nguoiDung = nguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng"));

        // Validate đối tượng tồn tại
        validateDoiTuongExists(loaiDoiTuong, doiTuongId);

        // Tìm đánh giá cũ (nếu có)
        Optional<DanhGia> existingDanhGia = danhGiaRepository.findByNguoiDungIdAndLoaiDoiTuongAndDoiTuongId(
                nguoiDungId, loaiDoiTuong, doiTuongId);

        DanhGia danhGia;
        if (existingDanhGia.isPresent()) {
            // Cập nhật đánh giá cũ
            danhGia = existingDanhGia.get();
            danhGia.setSoSao(soSao);
            danhGia.setNoiDung(noiDung);
            danhGia.setTrangThai(TrangThaiDanhGia.HOAT_DONG);
        } else {
            // Tạo đánh giá mới
            danhGia = DanhGia.builder()
                    .nguoiDung(nguoiDung)
                    .loaiDoiTuong(loaiDoiTuong)
                    .doiTuongId(doiTuongId)
                    .soSao(soSao)
                    .noiDung(noiDung)
                    .trangThai(TrangThaiDanhGia.HOAT_DONG)
                    .build();
        }

        danhGia = danhGiaRepository.save(danhGia);

        // Cập nhật thống kê vào bảng đối tượng
        updateDoiTuongStats(loaiDoiTuong, doiTuongId);

        return danhGia;
    }

    /**
     * Lấy đánh giá của user cho một đối tượng
     */
    @Override
    public Optional<DanhGia> getMyDanhGia(Long nguoiDungId, LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId) {
        return danhGiaRepository.findByNguoiDungIdAndLoaiDoiTuongAndDoiTuongId(nguoiDungId, loaiDoiTuong, doiTuongId);
    }

    /**
     * Lấy danh sách đánh giá cho một đối tượng (phân trang)
     */
    public Page<DanhGia> getDanhGiaByDoiTuong(LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId, Pageable pageable) {
        return danhGiaRepository.findByLoaiDoiTuongAndDoiTuongIdAndTrangThai(
                loaiDoiTuong, doiTuongId, TrangThaiDanhGia.HOAT_DONG, pageable);
    }

    /**
     * Lấy thống kê đánh giá cho một đối tượng
     */
    @Override
    public Map<String, Object> getStatsForDoiTuong(LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId) {
        Map<String, Object> stats = new HashMap<>();

        // Tổng số đánh giá
        long total = danhGiaRepository.countByLoaiDoiTuongAndDoiTuongIdAndTrangThai(
                loaiDoiTuong, doiTuongId, TrangThaiDanhGia.HOAT_DONG);
        stats.put("tongDanhGia", total);

        // Trung bình sao
        Double avgRating = danhGiaRepository.calculateAverageRating(
                loaiDoiTuong, doiTuongId, TrangThaiDanhGia.HOAT_DONG);
        stats.put("trungBinhSao", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);

        // Phân bố số sao
        List<Object[]> distribution = danhGiaRepository.countBySoSao(loaiDoiTuong, doiTuongId, TrangThaiDanhGia.HOAT_DONG);
        Map<Integer, Long> starDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            starDistribution.put(i, 0L);
        }
        for (Object[] row : distribution) {
            Integer star = (Integer) row[0];
            Long count = (Long) row[1];
            starDistribution.put(star, count);
        }
        stats.put("phanBoSao", starDistribution);

        return stats;
    }

    /**
     * Xóa đánh giá (soft delete)
     */
    @Override
    @Transactional
    public void deleteDanhGia(Long danhGiaId, Long nguoiDungId) throws DataNotFoundException {
        DanhGia danhGia = danhGiaRepository.findById(danhGiaId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy đánh giá"));

        if (!danhGia.getNguoiDung().getId().equals(nguoiDungId)) {
            throw new DataNotFoundException("Không có quyền xóa đánh giá này");
        }

        danhGia.setTrangThai(TrangThaiDanhGia.DA_XOA);
        danhGiaRepository.save(danhGia);

        // Cập nhật lại thống kê
        updateDoiTuongStats(danhGia.getLoaiDoiTuong(), danhGia.getDoiTuongId());
    }

    /**
     * Validate đối tượng tồn tại
     */
    private void validateDoiTuongExists(LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId) throws DataNotFoundException {
        if (loaiDoiTuong == LoaiDoiTuongDanhGia.BO_CAU_HOI) {
            if (!boCauHoiRepository.existsById(doiTuongId)) {
                throw new DataNotFoundException("Không tìm thấy bộ câu hỏi");
            }
        } else if (loaiDoiTuong == LoaiDoiTuongDanhGia.KHOA_HOC) {
            if (!khoaHocRepository.existsById(doiTuongId)) {
                throw new DataNotFoundException("Không tìm thấy khóa học");
            }
        }
    }

    /**
     * Cập nhật thống kê vào bảng đối tượng
     */
    private void updateDoiTuongStats(LoaiDoiTuongDanhGia loaiDoiTuong, Long doiTuongId) {
        long total = danhGiaRepository.countByLoaiDoiTuongAndDoiTuongIdAndTrangThai(
                loaiDoiTuong, doiTuongId, TrangThaiDanhGia.HOAT_DONG);
        Double avgRating = danhGiaRepository.calculateAverageRating(
                loaiDoiTuong, doiTuongId, TrangThaiDanhGia.HOAT_DONG);

        double roundedAvg = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;

        if (loaiDoiTuong == LoaiDoiTuongDanhGia.BO_CAU_HOI) {
            boCauHoiRepository.findById(doiTuongId).ifPresent(boCauHoi -> {
                boCauHoi.setTongDanhGia((int) total);
                boCauHoi.setTrungBinhSao(roundedAvg);
                boCauHoiRepository.save(boCauHoi);
            });
        } else if (loaiDoiTuong == LoaiDoiTuongDanhGia.KHOA_HOC) {
            khoaHocRepository.findById(doiTuongId).ifPresent(khoaHoc -> {
                khoaHoc.setTongDanhGia((int) total);
                khoaHoc.setTrungBinhSao(roundedAvg);
                khoaHocRepository.save(khoaHoc);
            });
        }
    }
}
