package com.app.backend.services.bangxephang;

import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.models.BangXepHang;
import com.app.backend.models.enums.RankTier;
import com.app.backend.responses.bangxephang.LeaderboardEntryResponse;
import com.app.backend.responses.bangxephang.WeeklyRankRewardResponse;
import com.app.backend.responses.user.UserSummaryResponse;
import com.app.backend.utils.XpCalculator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface IBangXepHangService {
    Page<LeaderboardEntryResponse> getGlobalLeaderboard(
            PageRequest pageRequest,
            String timeRange,
            Long chuDeId,
            Long boCauHoiId
//            Long currentUserId,
//            boolean friendOnly
    );

    UserSummaryResponse getUserSummary(Long userId) throws DataNotFoundException;

    void recalcAllRankings();

    XpCalculator.LevelInfo computeLevelInfo(long totalXp);

    RankTier getRankTier(BangXepHang bxh);

    RankTier calculateRankTier(int totalPoints);

    // XP nhận từ 1 trận (legacy - deprecated)
    @Deprecated
    long calculateXpFromMatch(int score, boolean win);

    // XP nhận từ 1 trận - dựa vào số câu đúng
    long calculateXpFromMatch(int correctAnswers, int totalQuestions, boolean win, boolean isSpeedMode);

    // Vàng nhận từ 1 trận (legacy - deprecated)
    @Deprecated
    long calculateGoldFromMatch(int score, boolean win, boolean isRanked, RankTier rankTier);

    // Vàng nhận từ 1 trận - dựa vào số câu đúng
    long calculateGoldFromMatch(int correctAnswers, boolean win, boolean isRanked, RankTier rankTier, boolean isSpeedMode);

    WeeklyRankRewardResponse claimWeeklyReward(Long userId) throws Exception;

}
