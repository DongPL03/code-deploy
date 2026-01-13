package com.app.backend.responses.trandau;

import com.app.backend.models.enums.RankTier;
import com.app.backend.responses.LevelUpResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchRewardResponse {

    @JsonProperty("xp_gained")
    private Long xpGained;

    @JsonProperty("gold_gained")
    private Long goldGained;

    @JsonProperty("level_before")
    private Integer levelBefore;

    @JsonProperty("level_after")
    private Integer levelAfter;

    @JsonProperty("rank_tier_before")
    private RankTier rankTierBefore;

    @JsonProperty("rank_tier_after")
    private RankTier rankTierAfter;

    /**
     * Danh sách phần thưởng lên cấp (nếu có)
     */
    @JsonProperty("level_up_rewards")
    private List<LevelUpResponse.RewardItem> levelUpRewards;

    /**
     * Có lên cấp không
     */
    @JsonProperty("leveled_up")
    private Boolean leveledUp;
}
