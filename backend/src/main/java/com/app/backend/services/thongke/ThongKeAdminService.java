package com.app.backend.services.thongke;

import com.app.backend.models.NguoiDung;
import com.app.backend.models.constant.TrangThaiTranDau;
import com.app.backend.repositories.IBoCauHoiRepository;
import com.app.backend.repositories.ICauHoiRepository;
import com.app.backend.repositories.IKhoaHocRepository;
import com.app.backend.repositories.ILichSuTranDauRepository;
import com.app.backend.repositories.INguoiDungRepository;
import com.app.backend.repositories.ITranDauRepository;
import com.app.backend.responses.thongke.AdminSummaryStatsResponse;
import com.app.backend.responses.thongke.DateCountResponse;
import com.app.backend.responses.thongke.RatingOverviewStatsResponse;
import com.app.backend.responses.thongke.RatingStatsItemResponse;
import com.app.backend.responses.thongke.TopBoCauHoiStatsResponse;
import com.app.backend.responses.thongke.TopPlayerStatsResponse;
import com.app.backend.repositories.IDanhGiaRepository;
import com.app.backend.models.enums.TrangThaiDanhGia;
import com.app.backend.models.enums.LoaiDoiTuongDanhGia;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThongKeAdminService implements IThongKeAdminService {

    private final INguoiDungRepository nguoiDungRepository;
    private final ITranDauRepository tranDauRepository;
    private final IBoCauHoiRepository boCauHoiRepository;
    private final ICauHoiRepository cauHoiRepository;
    private final IKhoaHocRepository khoaHocRepository;
    private final ILichSuTranDauRepository lichSuTranDauRepository;
    private final IDanhGiaRepository danhGiaRepository;

    @Override
    public AdminSummaryStatsResponse getSummary() {
        // ========== NGƯỜI DÙNG ==========
        long tongNguoiDung = nguoiDungRepository.count();
        long nguoiDungActive = nguoiDungRepository.countActive();
        long nguoiDungBlocked = nguoiDungRepository.countBlocked();
        long nguoiDungDeleted = nguoiDungRepository.countDeleted();
        long soAdmin = nguoiDungRepository.countAdmins();
        
        // User mới hôm nay
        Instant startOfToday = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        long nguoiDungMoiHomNay = nguoiDungRepository.countRegisteredToday(startOfToday);

        // ========== TRẬN ĐẤU ==========
        long tongTranDau = tranDauRepository.count();
        long tranDangCho = tranDauRepository.countByTrangThai(TrangThaiTranDau.CHO);
        long tranDangDienRa = tranDauRepository.countByTrangThai(TrangThaiTranDau.DANG_CHOI);
        long tranDaKetThuc = tranDauRepository.countByTrangThai(TrangThaiTranDau.HOAN_THANH);
        long tranDaHuy = tranDauRepository.countByTrangThai(TrangThaiTranDau.HUY);
        
        // Trận hôm nay
        long tranHomNay = tranDauRepository.countCreatedToday(startOfToday);

        // ========== BỘ CÂU HỎI / CÂU HỎI ==========
        long tongBoCauHoi = boCauHoiRepository.count();
        long boCauHoiDaDuyet = boCauHoiRepository.countByTrangThai("DA_DUYET");
        long boCauHoiChoDuyet = boCauHoiRepository.countByTrangThai("CHO_DUYET");
        long boCauHoiTuChoi = boCauHoiRepository.countByTrangThai("TU_CHOI");
        long tongCauHoi = cauHoiRepository.count();

        // ========== KHÓA HỌC ==========
        long tongKhoaHoc = khoaHocRepository.count();
        long khoaHocPublished = khoaHocRepository.countByTrangThai("PUBLISHED");
        long khoaHocDraft = khoaHocRepository.countByTrangThai("DRAFT");

        return AdminSummaryStatsResponse.builder()
                // Người dùng
                .tongNguoiDung(tongNguoiDung)
                .nguoiDungActive(nguoiDungActive)
                .nguoiDungBlocked(nguoiDungBlocked)
                .nguoiDungDeleted(nguoiDungDeleted)
                .soAdmin(soAdmin)
                .nguoiDungMoiHomNay(nguoiDungMoiHomNay)
                // Trận đấu
                .tongTranDau(tongTranDau)
                .tranDangCho(tranDangCho)
                .tranDangDienRa(tranDangDienRa)
                .tranDaKetThuc(tranDaKetThuc)
                .tranDaHuy(tranDaHuy)
                .tranHomNay(tranHomNay)
                // Bộ câu hỏi
                .tongBoCauHoi(tongBoCauHoi)
                .boCauHoiDaDuyet(boCauHoiDaDuyet)
                .boCauHoiChoDuyet(boCauHoiChoDuyet)
                .boCauHoiTuChoi(boCauHoiTuChoi)
                .tongCauHoi(tongCauHoi)
                // Khóa học
                .tongKhoaHoc(tongKhoaHoc)
                .khoaHocPublished(khoaHocPublished)
                .khoaHocDraft(khoaHocDraft)
                .build();
    }

    @Override
    public List<DateCountResponse> getBattlesByDay(int days) {
        if (days <= 0) days = 7;
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);

        List<Object[]> raw = tranDauRepository.countBattlesByDaySince(from);

        return raw.stream().map(row -> {
            Object dateObj = row[0];
            LocalDate date;

            if (dateObj instanceof LocalDate localDate) {
                date = localDate;
            } else if (dateObj instanceof java.sql.Date sqlDate) {
                date = sqlDate.toLocalDate();
            } else if (dateObj instanceof java.util.Date utilDate) {
                date = utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                throw new IllegalStateException("Không parse được kiểu ngày cho thống kê trận");
            }

            long count = (row[1] instanceof Number num) ? num.longValue() : 0L;

            return DateCountResponse.builder()
                    .ngay(date)
                    .soLuong(count)
                    .build();
        }).toList();
    }

    @Override
    public List<TopBoCauHoiStatsResponse> getTopBoCauHoi(int limit) {
        if (limit <= 0) limit = 5;

        var page = tranDauRepository.findTopBoCauHoiByBattleCount(
                TrangThaiTranDau.HOAN_THANH,
                PageRequest.of(0, limit)
        );

        return page.stream()
                .map(row -> {
                    Long boCauHoiId = (Long) row[0];
                    String tieuDe = (String) row[1];
                    long soTran = (row[2] instanceof Number num) ? num.longValue() : 0L;

                    return TopBoCauHoiStatsResponse.builder()
                            .boCauHoiId(boCauHoiId)
                            .tieuDe(tieuDe)
                            .soTran(soTran)
                            .build();
                })
                .toList();
    }

    @Override
    public List<TopPlayerStatsResponse> getTopPlayers(int limit) {
        if (limit <= 0) limit = 10;

        var pageAgg = lichSuTranDauRepository.aggregateLeaderboard(
                null, // from - không giới hạn thời gian
                null, // to
                null, // chuDeId
                null, // boCauHoiId
                PageRequest.of(0, limit)
        );

        return pageAgg.stream()
                .map(agg -> {
                    Long userId = agg.getUserId();
                    NguoiDung user = nguoiDungRepository.getReferenceById(userId);

                    long tongDiem = agg.getTongDiem() != null ? agg.getTongDiem().longValue() : 0L;
                    long tongTran = agg.getTongTran() != null ? agg.getTongTran().longValue() : 0L;
                    long soThang = agg.getSoTranThang() != null ? agg.getSoTranThang().longValue() : 0L;

                    double tiLeThang = tongTran > 0 ? (soThang * 100.0 / tongTran) : 0.0;

                    return TopPlayerStatsResponse.builder()
                            .userId(userId)
                            .hoTen(user.getHoTen())
                            .tenDangNhap(user.getTenDangNhap())
                            .avatarUrl(user.getAvatarUrl())
                            .tongDiem(tongDiem)
                            .tongTran(tongTran)
                            .soTranThang(soThang)
                            .tiLeThang(Math.round(tiLeThang * 100.0) / 100.0)
                            .build();
                })
                .toList();
    }

    @Override
    public RatingOverviewStatsResponse getRatingStats(int limit) {
        if (limit <= 0) limit = 5;

        // Tổng số đánh giá
        long tongDanhGiaBoCauHoi = danhGiaRepository.countByLoaiDoiTuongAndTrangThai(
                LoaiDoiTuongDanhGia.BO_CAU_HOI, TrangThaiDanhGia.HOAT_DONG);
        long tongDanhGiaKhoaHoc = danhGiaRepository.countByLoaiDoiTuongAndTrangThai(
                LoaiDoiTuongDanhGia.KHOA_HOC, TrangThaiDanhGia.HOAT_DONG);

        // Trung bình sao
        Double avgBoCauHoi = danhGiaRepository.calculateAverageByLoaiDoiTuong(
                LoaiDoiTuongDanhGia.BO_CAU_HOI, TrangThaiDanhGia.HOAT_DONG);
        Double avgKhoaHoc = danhGiaRepository.calculateAverageByLoaiDoiTuong(
                LoaiDoiTuongDanhGia.KHOA_HOC, TrangThaiDanhGia.HOAT_DONG);

        // Top rated bộ câu hỏi
        var topRatedBoCauHoi = boCauHoiRepository.findTopRated(PageRequest.of(0, limit))
                .stream()
                .map(bo -> RatingStatsItemResponse.builder()
                        .id(bo.getId())
                        .tieuDe(bo.getTieuDe())
                        .chuDe(bo.getChuDe() != null ? bo.getChuDe().getTen() : null)
                        .nguoiTao(bo.getTaoBoi() != null ? bo.getTaoBoi().getHoTen() : null)
                        .trungBinhSao(bo.getTrungBinhSao())
                        .tongDanhGia(bo.getTongDanhGia())
                        .loai("BO_CAU_HOI")
                        .build())
                .toList();

        // Top rated khóa học
        var topRatedKhoaHoc = khoaHocRepository.findTopRated(PageRequest.of(0, limit))
                .stream()
                .map(kh -> RatingStatsItemResponse.builder()
                        .id(kh.getId())
                        .tieuDe(kh.getTieuDe())
                        .chuDe(kh.getChuDe() != null ? kh.getChuDe().getTen() : null)
                        .nguoiTao(kh.getTaoBoi() != null ? kh.getTaoBoi().getHoTen() : null)
                        .trungBinhSao(kh.getTrungBinhSao())
                        .tongDanhGia(kh.getTongDanhGia())
                        .loai("KHOA_HOC")
                        .build())
                .toList();

        // Lowest rated bộ câu hỏi
        var lowestRatedBoCauHoi = boCauHoiRepository.findLowestRated(PageRequest.of(0, limit))
                .stream()
                .map(bo -> RatingStatsItemResponse.builder()
                        .id(bo.getId())
                        .tieuDe(bo.getTieuDe())
                        .chuDe(bo.getChuDe() != null ? bo.getChuDe().getTen() : null)
                        .nguoiTao(bo.getTaoBoi() != null ? bo.getTaoBoi().getHoTen() : null)
                        .trungBinhSao(bo.getTrungBinhSao())
                        .tongDanhGia(bo.getTongDanhGia())
                        .loai("BO_CAU_HOI")
                        .build())
                .toList();

        // Lowest rated khóa học
        var lowestRatedKhoaHoc = khoaHocRepository.findLowestRated(PageRequest.of(0, limit))
                .stream()
                .map(kh -> RatingStatsItemResponse.builder()
                        .id(kh.getId())
                        .tieuDe(kh.getTieuDe())
                        .chuDe(kh.getChuDe() != null ? kh.getChuDe().getTen() : null)
                        .nguoiTao(kh.getTaoBoi() != null ? kh.getTaoBoi().getHoTen() : null)
                        .trungBinhSao(kh.getTrungBinhSao())
                        .tongDanhGia(kh.getTongDanhGia())
                        .loai("KHOA_HOC")
                        .build())
                .toList();

        // Most reviewed bộ câu hỏi
        var mostReviewedBoCauHoi = boCauHoiRepository.findMostReviewed(PageRequest.of(0, limit))
                .stream()
                .map(bo -> RatingStatsItemResponse.builder()
                        .id(bo.getId())
                        .tieuDe(bo.getTieuDe())
                        .chuDe(bo.getChuDe() != null ? bo.getChuDe().getTen() : null)
                        .nguoiTao(bo.getTaoBoi() != null ? bo.getTaoBoi().getHoTen() : null)
                        .trungBinhSao(bo.getTrungBinhSao())
                        .tongDanhGia(bo.getTongDanhGia())
                        .loai("BO_CAU_HOI")
                        .build())
                .toList();

        // Most reviewed khóa học
        var mostReviewedKhoaHoc = khoaHocRepository.findMostReviewed(PageRequest.of(0, limit))
                .stream()
                .map(kh -> RatingStatsItemResponse.builder()
                        .id(kh.getId())
                        .tieuDe(kh.getTieuDe())
                        .chuDe(kh.getChuDe() != null ? kh.getChuDe().getTen() : null)
                        .nguoiTao(kh.getTaoBoi() != null ? kh.getTaoBoi().getHoTen() : null)
                        .trungBinhSao(kh.getTrungBinhSao())
                        .tongDanhGia(kh.getTongDanhGia())
                        .loai("KHOA_HOC")
                        .build())
                .toList();

        return RatingOverviewStatsResponse.builder()
                .tongDanhGiaBoCauHoi(tongDanhGiaBoCauHoi)
                .tongDanhGiaKhoaHoc(tongDanhGiaKhoaHoc)
                .trungBinhSaoBoCauHoi(avgBoCauHoi != null ? Math.round(avgBoCauHoi * 10.0) / 10.0 : 0.0)
                .trungBinhSaoKhoaHoc(avgKhoaHoc != null ? Math.round(avgKhoaHoc * 10.0) / 10.0 : 0.0)
                .topRatedBoCauHoi(topRatedBoCauHoi)
                .topRatedKhoaHoc(topRatedKhoaHoc)
                .lowestRatedBoCauHoi(lowestRatedBoCauHoi)
                .lowestRatedKhoaHoc(lowestRatedKhoaHoc)
                .mostReviewedBoCauHoi(mostReviewedBoCauHoi)
                .mostReviewedKhoaHoc(mostReviewedKhoaHoc)
                .build();
    }
}
