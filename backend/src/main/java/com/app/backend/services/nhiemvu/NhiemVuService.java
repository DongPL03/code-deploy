package com.app.backend.services.nhiemvu;

import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.models.BangXepHang;
import com.app.backend.models.TienDoNhiemVu;
import com.app.backend.models.VatPham;
import com.app.backend.models.enums.LoaiNhiemVu;
import com.app.backend.models.enums.LoaiVatPham;
import com.app.backend.models.enums.MaNhiemVu;
import com.app.backend.repositories.IBangXepHangRepository;
import com.app.backend.repositories.ITienDoNhiemVuRepository;
import com.app.backend.repositories.IVatPhamRepository;
import com.app.backend.repositories.INguoiDungRepository;
import com.app.backend.responses.NhiemVuResponse;
import com.app.backend.responses.NhanThuongNhiemVuResponse;
import com.app.backend.services.vatpham.IVatPhamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhiemVuService implements INhiemVuService {

    private final ITienDoNhiemVuRepository tienDoNhiemVuRepository;
    private final IBangXepHangRepository bangXepHangRepository;
    private final INguoiDungRepository nguoiDungRepository;
    private final IVatPhamRepository vatPhamRepository;
    private final IVatPhamService vatPhamService;

    // ================== HELPER: Tính ngày bắt đầu chu kỳ ==================

    private LocalDate getDailyStartDate() {
        return LocalDate.now();
    }

    private LocalDate getWeeklyStartDate() {
        // Thứ 2 của tuần hiện tại
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate getStartDateForQuest(MaNhiemVu ma) {
        return ma.getLoai() == LoaiNhiemVu.HANG_NGAY ? getDailyStartDate() : getWeeklyStartDate();
    }

    // ================== LẤY DANH SÁCH NHIỆM VỤ ==================

    @Override
    @Transactional(readOnly = true)
    public NhiemVuResponse getQuests(Long userId) {
        LocalDate dailyStart = getDailyStartDate();
        LocalDate weeklyStart = getWeeklyStartDate();

        // Lấy tất cả tiến độ DAILY
        List<MaNhiemVu> dailyMas = Arrays.asList(MaNhiemVu.getByLoai(LoaiNhiemVu.HANG_NGAY));
        List<TienDoNhiemVu> dailyProgress = tienDoNhiemVuRepository
                .findByUserAndMaNhiemVusAndNgay(userId, dailyMas, dailyStart);
        Map<MaNhiemVu, TienDoNhiemVu> dailyMap = dailyProgress.stream()
                .collect(Collectors.toMap(TienDoNhiemVu::getMaNhiemVu, t -> t));

        // Lấy tất cả tiến độ WEEKLY
        List<MaNhiemVu> weeklyMas = Arrays.asList(MaNhiemVu.getByLoai(LoaiNhiemVu.HANG_TUAN));
        List<TienDoNhiemVu> weeklyProgress = tienDoNhiemVuRepository
                .findByUserAndMaNhiemVusAndNgay(userId, weeklyMas, weeklyStart);
        Map<MaNhiemVu, TienDoNhiemVu> weeklyMap = weeklyProgress.stream()
                .collect(Collectors.toMap(TienDoNhiemVu::getMaNhiemVu, t -> t));

        // Build response
        List<NhiemVuResponse.QuestItem> dailyItems = dailyMas.stream()
                .map(ma -> NhiemVuResponse.QuestItem.from(ma, dailyMap.get(ma)))
                .collect(Collectors.toList());

        List<NhiemVuResponse.QuestItem> weeklyItems = weeklyMas.stream()
                .map(ma -> NhiemVuResponse.QuestItem.from(ma, weeklyMap.get(ma)))
                .collect(Collectors.toList());

        int dailyCompleted = (int) dailyItems.stream().filter(NhiemVuResponse.QuestItem::isDaHoanThanh).count();
        int weeklyCompleted = (int) weeklyItems.stream().filter(NhiemVuResponse.QuestItem::isDaHoanThanh).count();

        return NhiemVuResponse.builder()
                .dailyQuests(dailyItems)
                .weeklyQuests(weeklyItems)
                .dailyCompleted(dailyCompleted)
                .dailyTotal(dailyItems.size())
                .weeklyCompleted(weeklyCompleted)
                .weeklyTotal(weeklyItems.size())
                .build();
    }

    // ================== NHẬN THƯỞNG ==================

    @Override
    @Transactional
    public NhanThuongNhiemVuResponse claimReward(Long userId, MaNhiemVu maNhiemVu) throws DataNotFoundException {
        LocalDate startDate = getStartDateForQuest(maNhiemVu);

        TienDoNhiemVu tienDo = tienDoNhiemVuRepository
                .findByNguoiDung_IdAndMaNhiemVuAndNgayBatDau(userId, maNhiemVu, startDate)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy tiến độ nhiệm vụ"));

        if (!tienDo.getDaHoanThanh()) {
            return NhanThuongNhiemVuResponse.builder()
                    .thanhCong(false)
                    .thongBao("Nhiệm vụ chưa hoàn thành!")
                    .build();
        }

        if (tienDo.getDaNhanThuong()) {
            return NhanThuongNhiemVuResponse.builder()
                    .thanhCong(false)
                    .thongBao("Đã nhận thưởng rồi!")
                    .build();
        }

        // Trao thưởng
        List<NhanThuongNhiemVuResponse.RewardItem> rewards = grantRewards(userId, maNhiemVu);

        // Đánh dấu đã nhận
        tienDo.setDaNhanThuong(true);
        tienDoNhiemVuRepository.save(tienDo);

        // Lấy gold mới
        Long goldMoi = bangXepHangRepository.findByNguoiDungId(userId)
                .map(BangXepHang::getTienVang).orElse(0L);

        return NhanThuongNhiemVuResponse.builder()
                .thanhCong(true)
                .thongBao("🎉 Nhận thưởng thành công!")
                .phanThuong(rewards)
                .goldMoi(goldMoi)
                .build();
    }

    @Override
    @Transactional
    public NhanThuongNhiemVuResponse claimAllRewards(Long userId) throws DataNotFoundException {
        List<TienDoNhiemVu> unclaimed = tienDoNhiemVuRepository
                .findByNguoiDung_IdAndDaHoanThanhTrueAndDaNhanThuongFalse(userId);

        if (unclaimed.isEmpty()) {
            return NhanThuongNhiemVuResponse.builder()
                    .thanhCong(false)
                    .thongBao("Không có nhiệm vụ nào để nhận thưởng!")
                    .build();
        }

        List<NhanThuongNhiemVuResponse.RewardItem> allRewards = new ArrayList<>();

        for (TienDoNhiemVu tienDo : unclaimed) {
            List<NhanThuongNhiemVuResponse.RewardItem> rewards = grantRewards(userId, tienDo.getMaNhiemVu());
            allRewards.addAll(rewards);
            tienDo.setDaNhanThuong(true);
        }

        tienDoNhiemVuRepository.saveAll(unclaimed);

        Long goldMoi = bangXepHangRepository.findByNguoiDungId(userId)
                .map(BangXepHang::getTienVang).orElse(0L);

        return NhanThuongNhiemVuResponse.builder()
                .thanhCong(true)
                .thongBao("🎉 Đã nhận " + unclaimed.size() + " phần thưởng!")
                .phanThuong(allRewards)
                .goldMoi(goldMoi)
                .build();
    }

    /**
     * Trao phần thưởng cho nhiệm vụ
     */
    private List<NhanThuongNhiemVuResponse.RewardItem> grantRewards(Long userId, MaNhiemVu maNhiemVu) 
            throws DataNotFoundException {
        List<NhanThuongNhiemVuResponse.RewardItem> rewards = new ArrayList<>();

        BangXepHang bxh = bangXepHangRepository.findByNguoiDungId(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy bảng xếp hạng"));

        // Gold
        if (maNhiemVu.getGoldThuong() > 0) {
            bxh.setTienVang(bxh.getTienVang() + maNhiemVu.getGoldThuong());
            rewards.add(NhanThuongNhiemVuResponse.RewardItem.builder()
                    .loai("GOLD")
                    .ten("Xu")
                    .soLuong(maNhiemVu.getGoldThuong())
                    .icon("💰")
                    .build());
            log.info("💰 User {} nhận {} gold từ nhiệm vụ {}", userId, maNhiemVu.getGoldThuong(), maNhiemVu);
        }

        // XP (bonus)
        if (maNhiemVu.getXpThuong() > 0) {
            bxh.setTongXp(bxh.getTongXp() + maNhiemVu.getXpThuong());
            rewards.add(NhanThuongNhiemVuResponse.RewardItem.builder()
                    .loai("XP")
                    .ten("Kinh nghiệm")
                    .soLuong(maNhiemVu.getXpThuong())
                    .icon("⚡")
                    .build());
            log.info("⚡ User {} nhận {} XP từ nhiệm vụ {}", userId, maNhiemVu.getXpThuong(), maNhiemVu);
        }

        // Vật phẩm
        if (maNhiemVu.getVatPhamLoai() != null) {
            try {
                LoaiVatPham loaiVp = LoaiVatPham.valueOf(maNhiemVu.getVatPhamLoai());
                VatPham vatPham = vatPhamRepository.findByLoai(loaiVp).orElse(null);
                if (vatPham != null) {
                    vatPhamService.grantItemToUser(userId, vatPham.getId(), 1);
                    rewards.add(NhanThuongNhiemVuResponse.RewardItem.builder()
                            .loai("VAT_PHAM")
                            .ten(vatPham.getTen())
                            .soLuong(1)
                            .icon(vatPham.getIcon())
                            .build());
                    log.info("🎁 User {} nhận {} từ nhiệm vụ {}", userId, vatPham.getTen(), maNhiemVu);
                }
            } catch (Exception e) {
                log.error("Lỗi trao vật phẩm từ nhiệm vụ: {}", e.getMessage());
            }
        }

        bangXepHangRepository.save(bxh);
        return rewards;
    }

    // ================== CẬP NHẬT TIẾN ĐỘ ==================

    @Override
    @Transactional
    public void onMatchPlayed(Long userId, boolean isRanked) {
        // DAILY: Tham gia trận
        updateProgress(userId, MaNhiemVu.NGAY_CHOI_1, 1);
        updateProgress(userId, MaNhiemVu.NGAY_CHOI_3, 1);

        // WEEKLY: Tham gia trận
        updateProgress(userId, MaNhiemVu.TUAN_CHOI_10, 1);
        updateProgress(userId, MaNhiemVu.TUAN_CHOI_20, 1);

        log.debug("📊 Updated match played progress for user {}", userId);
    }

    @Override
    @Transactional
    public void onMatchWon(Long userId, boolean isRanked) {
        // DAILY: Thắng trận
        updateProgress(userId, MaNhiemVu.NGAY_THANG_1, 1);

        // WEEKLY: Thắng trận
        updateProgress(userId, MaNhiemVu.TUAN_THANG_5, 1);
        updateProgress(userId, MaNhiemVu.TUAN_THANG_10, 1);

        // WEEKLY: Thắng trận Ranked
        if (isRanked) {
            updateProgress(userId, MaNhiemVu.TUAN_RANKED_THANG_3, 1);
        }

        log.debug("🏆 Updated match won progress for user {}", userId);
    }

    @Override
    @Transactional
    public void onCorrectAnswer(Long userId, int count) {
        // DAILY: Trả lời đúng
        updateProgress(userId, MaNhiemVu.NGAY_DUNG_10, count);
        updateProgress(userId, MaNhiemVu.NGAY_DUNG_30, count);

        // WEEKLY: Trả lời đúng
        updateProgress(userId, MaNhiemVu.TUAN_DUNG_100, count);
        updateProgress(userId, MaNhiemVu.TUAN_DUNG_200, count);

        log.debug("✅ Updated correct answer progress for user {}: +{}", userId, count);
    }

    @Override
    @Transactional
    public void onComboAchieved(Long userId, int comboCount) {
        // Chỉ cập nhật nếu combo >= mục tiêu
        if (comboCount >= MaNhiemVu.NGAY_COMBO_5.getMucTieu()) {
            updateProgress(userId, MaNhiemVu.NGAY_COMBO_5, 1);
            log.debug("🔥 User {} đạt combo {}", userId, comboCount);
        }
    }

    @Override
    @Transactional
    public void onTop3Achieved(Long userId) {
        updateProgress(userId, MaNhiemVu.TUAN_TOP3_3, 1);
        log.debug("🥇 Updated top 3 progress for user {}", userId);
    }

    /**
     * Cập nhật tiến độ nhiệm vụ
     */
    private void updateProgress(Long userId, MaNhiemVu maNhiemVu, int amount) {
        LocalDate startDate = getStartDateForQuest(maNhiemVu);

        TienDoNhiemVu tienDo = tienDoNhiemVuRepository
                .findByNguoiDung_IdAndMaNhiemVuAndNgayBatDau(userId, maNhiemVu, startDate)
                .orElseGet(() -> TienDoNhiemVu.builder()
                        .nguoiDung(nguoiDungRepository.getReferenceById(userId))
                        .maNhiemVu(maNhiemVu)
                        .ngayBatDau(startDate)
                        .tienDo(0)
                        .daHoanThanh(false)
                        .daNhanThuong(false)
                        .build());

        // Nếu đã hoàn thành rồi thì không cần update nữa
        if (tienDo.getDaHoanThanh()) {
            return;
        }

        tienDo.tangTienDo(amount);

        if (tienDo.getDaHoanThanh()) {
            log.info("🎯 User {} hoàn thành nhiệm vụ: {}", userId, maNhiemVu.getMoTa());
        }

        tienDoNhiemVuRepository.save(tienDo);
    }
}
