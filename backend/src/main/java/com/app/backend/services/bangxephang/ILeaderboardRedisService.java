package com.app.backend.services.bangxephang;

import com.app.backend.responses.bangxephang.LeaderboardEntryResponse;
import org.springframework.data.domain.Page;

public interface ILeaderboardRedisService {
    /**
     * Get cached leaderboard
     * @param timeRange ALL, WEEK, MONTH
     * @param chuDeId optional topic ID
     * @param boCauHoiId optional question set ID
     * @param page page number
     * @param limit items per page
     * @return cached Page or null if miss
     */
    Page<LeaderboardEntryResponse> getLeaderboard(String timeRange, Long chuDeId, Long boCauHoiId, int page, int limit);

    /**
     * Cache leaderboard result
     */
    void saveLeaderboard(String timeRange, Long chuDeId, Long boCauHoiId, int page, int limit, Page<LeaderboardEntryResponse> data);

    /**
     * Clear all leaderboard cache (call after match ends)
     */
    void clearLeaderboardCache();

    /**
     * Clear specific leaderboard cache
     */
    void clearLeaderboardCache(String timeRange, Long chuDeId, Long boCauHoiId);
}
