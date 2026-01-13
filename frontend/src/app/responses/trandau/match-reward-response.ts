export interface LevelUpRewardItem {
  loai: 'VANG' | 'VAT_PHAM' | 'XP';
  ten: string;
  so_luong: number;
  icon: string;
  cap_do: number;
}

export interface MatchRewardResponse {
  xp_gained: number;
  gold_gained: number;
  level_before: number;
  level_after: number;
  rank_tier_before: 'DONG' | 'BAC' | 'VANG' | 'BACH_KIM' | 'KIM_CUONG' | 'CAO_THU';
  rank_tier_after: 'DONG' | 'BAC' | 'VANG' | 'BACH_KIM' | 'KIM_CUONG' | 'CAO_THU';
  leveled_up?: boolean;
  level_up_rewards?: LevelUpRewardItem[];
}
