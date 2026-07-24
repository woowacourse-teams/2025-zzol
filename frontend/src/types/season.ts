export type SeasonTier = 'BRONZE' | 'SILVER' | 'GOLD' | 'DIAMOND';

export type SeasonLeaderboardRow = {
  rank: number;
  nickname: string;
  userCode: string;
  totalPoints: number;
  tier: SeasonTier;
};

export type SeasonLeaderboardResponse = {
  seasonKey: string;
  totalMembers: number;
  rows: SeasonLeaderboardRow[];
};

export type SeasonRankResponse = {
  seasonKey: string;
  nickname: string;
  userCode: string;
  rank: number;
  totalPoints: number;
  tier: SeasonTier;
  totalMembers: number;
};

export type SeasonRankEntry = {
  playerName: string;
  totalPoints: number;
  tier: SeasonTier;
  seasonRank: number;
};

export type SeasonRankMessage = {
  seasonKey: string;
  entries: SeasonRankEntry[];
};
