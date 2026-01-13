package com.app.backend.services.bangxephang;

import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.models.BangXepHang;
import com.app.backend.models.LichSuTranDau;
import com.app.backend.models.NguoiDung;
import com.app.backend.models.enums.RankTier;
import com.app.backend.repositories.IBangXepHangRepository;
import com.app.backend.repositories.ILichSuTranDauRepository;
import com.app.backend.repositories.INguoiDungRepository;
import com.app.backend.repositories.ILeaderboardAggregateProjection;
import com.app.backend.responses.bangxephang.LeaderboardEntryResponse;
import com.app.backend.responses.bangxephang.WeeklyRankRewardResponse;
import com.app.backend.responses.lichsutrandau.LichSuTranDauResponse;
import com.app.backend.responses.user.UserSummaryResponse;
import com.app.backend.utils.XpCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BangXepHangService implements IBangXepHangService {

    private final IBangXepHangRepository bangXepHangRepository;
    private final ILichSuTranDauRepository lichSuTranDauRepository;
    private final INguoiDungRepository nguoiDungRepository;

    @Override
    public Page<LeaderboardEntryResponse> getGlobalLeaderboard(
            PageRequest pageRequest,
            String timeRange,
            Long chuDeId,
            Long boCauHoiId
//            Long currentUserId,
//            boolean friendOnly
    ) {

        boolean isAllMode = (timeRange == null || "ALL".equalsIgnoreCase(timeRange))
                && chuDeId == null
                && boCauHoiId == null;
//        && !friendOnly;

        if (isAllMode) {
            // ❖ mode ALL (mặc định): lấy trực tiếp từ bảng bang_xep_hang
            return mapFromBangXepHang(pageRequest);
        } else {
            // ❖ mode filter nâng cao: tính lại từ bảng lich_su_tran_dau
//            return mapFromLichSuTranDau(pageRequest, timeRange, chuDeId, boCauHoiId, currentUserId, friendOnly);
            return mapFromLichSuTranDau(pageRequest, timeRange, chuDeId, boCauHoiId);
        }
    }

    // ============================================================
    // 1. Mode ALL → dùng bảng bang_xep_hang
    // ============================================================
    private Page<LeaderboardEntryResponse> mapFromBangXepHang(PageRequest pageRequest) {
        Page<BangXepHang> page = bangXepHangRepository
                .findAllByOrderByTongDiemDescCapNhatLucAsc(pageRequest);

        AtomicInteger rankCounter =
                new AtomicInteger(pageRequest.getPageNumber() * pageRequest.getPageSize() + 1);

        return page.map(bxh -> {
            NguoiDung user = bxh.getNguoiDung();
            int tongTran = bxh.getTongTran() != null ? bxh.getTongTran() : 0;
            int soThang = bxh.getSoTranThang() != null ? bxh.getSoTranThang() : 0;
            int soThua = bxh.getSoTranThua() != null ? bxh.getSoTranThua() : 0;

            double winRate = 0.0;
            if (tongTran > 0) {
                winRate = soThang * 100.0 / tongTran;
            }

            int rank = rankCounter.getAndIncrement();
            int tongDiem = bxh.getTongDiem() != null ? bxh.getTongDiem() : 0;
            RankTier tier = calculateRankTier(tongDiem);

            return LeaderboardEntryResponse.builder()
                    .userId(user.getId())
                    .hoTen(user.getHoTen())
                    .anhDaiDien(user.getAvatarUrl())
                    .tongDiem(bxh.getTongDiem())
                    .tongTran(tongTran)
                    .soTranThang(soThang)
                    .soTranThua(soThua)
                    .tiLeThang(winRate)
                    .xepHang(bxh.getXepHang())
                    .rankTier(tier)
                    .level(bxh.getLevel())
                    .build();
        });
    }

    // ============================================================
    // 2. Mode filter nâng cao → tính từ lich_su_tran_dau
    // ============================================================
    private Page<LeaderboardEntryResponse> mapFromLichSuTranDau(
            PageRequest pageRequest,
            String timeRange,
            Long chuDeId,
            Long boCauHoiId
//            Long currentUserId,
//            boolean friendOnly
    ) {

        Instant now = Instant.now();
        Instant from = null;
        Instant to = null;

        if ("WEEK".equalsIgnoreCase(timeRange)) {
            from = now.minus(Duration.ofDays(7));
            to = now;
        } else if ("MONTH".equalsIgnoreCase(timeRange)) {
            from = now.minus(Duration.ofDays(30));
            to = now;
        } else {
            // ALL hoặc null → không giới hạn thời gian
            from = null;
            to = null;
        }

        // Hiện tại friendOnly chưa có bảng bạn bè → tạm thời bỏ qua
        // Sau này có FriendRepository thì filter ở tầng service sau.

        Page<ILeaderboardAggregateProjection> pageAgg =
                lichSuTranDauRepository.aggregateLeaderboard(
                        from,
                        to,
                        chuDeId,
                        boCauHoiId,
                        pageRequest
                );

        AtomicInteger rankCounter =
                new AtomicInteger(pageRequest.getPageNumber() * pageRequest.getPageSize() + 1);

        return pageAgg.map(agg -> {
            Long userId = agg.getUserId();
            NguoiDung user = nguoiDungRepository.getReferenceById(userId);

            int tongDiem = safeToInt(agg.getTongDiem());
            int tongTran = safeToInt(agg.getTongTran());
            int soThang = safeToInt(agg.getSoTranThang());
            int soThua = Math.max(0, tongTran - soThang);

            double winRate = 0.0;
            if (tongTran > 0) {
                winRate = soThang * 100.0 / tongTran;
            }

            int rank = rankCounter.getAndIncrement();
            RankTier tier = calculateRankTier(tongDiem);

            return LeaderboardEntryResponse.builder()
                    .userId(user.getId())
                    .hoTen(user.getHoTen())
                    .anhDaiDien(user.getAvatarUrl())
                    .tongDiem(tongDiem)
                    .tongTran(tongTran)
                    .soTranThang(soThang)
                    .soTranThua(soThua)
                    .tiLeThang(winRate)
                    .rankTier(tier)
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserSummary(Long userId) throws DataNotFoundException {
        // 1️⃣ Lấy thông tin user
        NguoiDung user = nguoiDungRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Người dùng không tồn tại"));

        // 2️⃣ Lấy record BXH nếu có
        BangXepHang bxh = bangXepHangRepository.findByNguoiDung_Id(userId)
                .orElse(null);

        int tongDiem = 0;
        int tongTran = 0;
        int soThang = 0;
        int soThua = 0;

        if (bxh != null) {
            tongDiem = bxh.getTongDiem() != null ? bxh.getTongDiem() : 0;
            tongTran = bxh.getTongTran() != null ? bxh.getTongTran() : 0;
            soThang = bxh.getSoTranThang() != null ? bxh.getSoTranThang() : 0;
            soThua = bxh.getSoTranThua() != null ? bxh.getSoTranThua() : 0;
        }

        double tiLeThang = 0.0;
        if (tongTran > 0) {
            tiLeThang = soThang * 100.0 / tongTran;
        }
        RankTier tier = calculateRankTier(tongDiem);
        Pageable pageable = PageRequest.of(0, 10);

        // Gọi hàm có sẵn trong ILichSuTranDauRepository bạn đã viết
        Page<LichSuTranDau> historyPage = lichSuTranDauRepository.findByNguoiDung_IdOrderByHoanThanhLucDesc(userId, pageable);

        // Map từ Entity sang DTO
        List<LichSuTranDauResponse> listLichSu = historyPage.getContent().stream()
                .map(LichSuTranDauResponse::fromEntity)
                .collect(Collectors.toList());


        // 3️⃣ Build response
        return UserSummaryResponse.from(
                bxh != null ? bxh : BangXepHang.builder().build(),
                user,
                listLichSu,
                this
        );
    }

    @Override
    @Transactional
    public void recalcAllRankings() {
        bangXepHangRepository.updateAllRankings();
    }

    @Override
    @Transactional
    public WeeklyRankRewardResponse claimWeeklyReward(Long userId) throws Exception {
        BangXepHang bxh = bangXepHangRepository.findByNguoiDung_Id(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy bảng xếp hạng của bạn"));

        String currentWeekId = getCurrentWeekId();
        String lastWeekId = bxh.getLastRankRewardWeek();

        long goldBefore = bxh.getTienVang() != null ? bxh.getTienVang() : 0L;

        // đã nhận trong tuần này rồi
        if (currentWeekId.equals(lastWeekId)) {
            return WeeklyRankRewardResponse.builder()
                    .claimedBefore(true)
                    .goldReward(0L)
                    .rankTier(bxh.getRankTier())
                    .globalRank(bxh.getXepHang())
                    .weekId(currentWeekId)
                    .goldBefore(goldBefore)
                    .goldAfter(goldBefore)
                    .build();
        }

        // tính thưởng
        long reward = computeWeeklyGoldReward(bxh);
        long goldAfter = goldBefore + reward;

        bxh.setTienVang(goldAfter);
        bxh.setLastRankRewardWeek(currentWeekId);
        bangXepHangRepository.save(bxh);

        return WeeklyRankRewardResponse.builder()
                .claimedBefore(false)
                .goldReward(reward)
                .rankTier(bxh.getRankTier())
                .globalRank(bxh.getXepHang())
                .weekId(currentWeekId)
                .goldBefore(goldBefore)
                .goldAfter(goldAfter)
                .build();
    }


    private int safeToInt(Long value) {
        return value != null ? value.intValue() : 0;
    }

    /**
     * Tính XP cần để lên level tiếp theo - delegate to XpCalculator
     */
    public long xpNeededForNextLevel(int level) {
        return XpCalculator.xpNeededForNextLevel(level);
    }

    /**
     * Tính thông tin level từ tổng XP - delegate to XpCalculator
     */
    public XpCalculator.LevelInfo computeLevelInfo(long totalXp) {
        return XpCalculator.computeLevelInfo(totalXp);
    }

    public RankTier calculateRankTier(int totalPoints) {
        return RankTier.fromPoints(totalPoints);
    }

    /**
     * Lấy RankTier hiện tại của 1 bản ghi BXH dựa trên tongDiem.
     */
    public RankTier getRankTier(BangXepHang bxh) {
        int points = bxh.getTongDiem() != null ? bxh.getTongDiem() : 0;
        return RankTier.fromPoints(points);
    }


    /**
     * Tính XP nhận từ 1 trận dựa trên SỐ CÂU ĐÚNGA + win bonus.
     * Cân bằng giữa chế độ THUONG và THUONG_TOC_DO.
     * 
     * @param correctAnswers Số câu trả lời đúng
     * @param totalQuestions Tổng số câu hỏi
     * @param win Thắng hay thua
     * @param isSpeedMode Chế độ THUONG_TOC_DO (có bonus nhỏ)
     * @return XP gained
     */
    public long calculateXpFromMatch(int correctAnswers, int totalQuestions, boolean win, boolean isSpeedMode) {
        if (correctAnswers <= 0) return 0;
        
        // Base XP: 50 XP mỗi câu đúng
        long baseXp = correctAnswers * 50L;
        
        // Accuracy bonus: +20% nếu đúng >= 80% câu, +10% nếu >= 60%
        double accuracyRate = totalQuestions > 0 ? (double) correctAnswers / totalQuestions : 0;
        double accuracyMultiplier = 1.0;
        if (accuracyRate >= 0.8) {
            accuracyMultiplier = 1.20;
        } else if (accuracyRate >= 0.6) {
            accuracyMultiplier = 1.10;
        }
        
        // Speed mode bonus: +15% cho chế độ tốc độ
        double speedMultiplier = isSpeedMode ? 1.15 : 1.0;
        
        // Win bonus
        long matchBonus = win ? 100L : 30L;
        
        long totalXp = Math.round(baseXp * accuracyMultiplier * speedMultiplier) + matchBonus;
        return Math.max(0, totalXp);
    }
    
    /**
     * Tính XP (backward compatible - dùng cho code cũ)
     * @deprecated Dùng calculateXpFromMatch(correctAnswers, totalQuestions, win, isSpeedMode) thay thế
     */
    @Override
    public long calculateXpFromMatch(int score, boolean win) {
        // Fallback: ước tính số câu đúng từ score (giả sử 100 điểm/câu cho CO_BAN)
        int estimatedCorrect = Math.max(1, score / 100);
        return calculateXpFromMatch(estimatedCorrect, estimatedCorrect, win, false);
    }

    /**
     * Tính Gold nhận từ 1 trận dựa trên SỐ CÂU ĐÚNGA.
     * Cân bằng giữa chế độ THUONG và THUONG_TOC_DO.
     */
    public long calculateGoldFromMatch(int correctAnswers, boolean win, boolean isRanked, RankTier rankTier, boolean isSpeedMode) {
        // Base gold: 5 gold mỗi câu đúng
        long baseGold = 5L + correctAnswers * 5L;
        
        // Ranked bonus
        long rankedBonus = 0L;
        if (isRanked) {
            rankedBonus = win ? 20L : 8L;
        }
        
        // Speed mode bonus: +10% (nhỏ hơn XP bonus)
        double speedMultiplier = isSpeedMode ? 1.10 : 1.0;
        
        // Rank tier multiplier
        double tierMulti = (rankTier != null) ? rankTier.getMultiplier() : 1.0;
        
        long total = Math.round((baseGold + rankedBonus) * speedMultiplier * tierMulti);
        return Math.max(0L, total);
    }

    /**
     * Tính Gold (backward compatible - dùng cho code cũ)
     * @deprecated Dùng calculateGoldFromMatch(correctAnswers, win, isRanked, rankTier, isSpeedMode) thay thế
     */
    @Override
    public long calculateGoldFromMatch(int score, boolean win, boolean isRanked, RankTier rankTier) {
        // Fallback: ước tính số câu đúng từ score
        int estimatedCorrect = Math.max(1, score / 100);
        return calculateGoldFromMatch(estimatedCorrect, win, isRanked, rankTier, false);
    }


    private String getCurrentWeekId() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        WeekFields wf = WeekFields.of(Locale.getDefault());
        int year = now.get(wf.weekBasedYear());
        int week = now.get(wf.weekOfWeekBasedYear());
        // ví dụ: 2025-05
        return String.format("%d-%02d", year, week);
    }

    private long computeWeeklyGoldReward(BangXepHang bxh) {
        RankTier tier = bxh.getRankTier() != null ? bxh.getRankTier() : RankTier.DONG;
        int globalRank = bxh.getXepHang() != null ? bxh.getXepHang() : Integer.MAX_VALUE;

        long base;
        switch (tier) {
            case BAC -> base = 100;
            case VANG -> base = 150;
            case BACH_KIM -> base = 200;
            case KIM_CUONG -> base = 300;
            case CAO_THU -> base = 400;
            default -> base = 50;
        }

        double multiplier = 1.0;
        if (globalRank == 1) {
            multiplier = 2.0;
        } else if (globalRank <= 3) {
            multiplier = 1.5;
        } else if (globalRank <= 10) {
            multiplier = 1.3;
        } else if (globalRank <= 50) {
            multiplier = 1.1;
        }

        return Math.round(base * multiplier);
    }


}
