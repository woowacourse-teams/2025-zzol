import type {
  Entry,
  SeasonRankMessage as WsSeasonRankMessage,
} from '@/apis/websocket/generated/wsContract';

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

export type SeasonRankEntry = Entry;

export type SeasonRankMessage = WsSeasonRankMessage;
