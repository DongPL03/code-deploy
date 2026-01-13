export interface WeeklyRankRewardResponse {
  gold_reward: number;
  rank_tier: 'DONG' | 'BAC' | 'VANG' | 'BACH_KIM' | 'KIM_CUONG' | 'CAO_THU';
  global_rank: number | null;
  week_id: string;      // ví dụ "2025-51"
  claimed_before: boolean;
  gold_before: number;
  gold_after: number;
}
