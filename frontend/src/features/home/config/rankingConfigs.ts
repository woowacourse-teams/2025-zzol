import type { TopWinner, GamePlayCount, LowestProbabilityWinner, BlockStackingTopPlayer } from '@/types/dashBoard';
import { MINI_GAME_NAME_MAP, type MiniGameType } from '@/types/miniGame/common';

export type RankingItem = {
  rank: number;
  name: string;
  count: number;
  unit?: string;
};

export type RankingCategory = {
  key: string;
  label: string;
  icon: string;
  endpoint: string;
  transformData: (raw: unknown) => RankingItem[];
};

export const RANKING_CATEGORIES: RankingCategory[] = [
  {
    key: 'top-winners',
    label: '이번 달 당첨 랭킹',
    icon: '🏆',
    endpoint: '/dashboard/top-winners',
    transformData: (raw) =>
      (raw as TopWinner[]).map((w, i) => ({
        rank: i + 1,
        name: w.playerName,
        count: w.winCount,
      })),
  },
  {
    key: 'lowest-probability',
    label: '최저 확률 당첨자',
    icon: '🍀',
    endpoint: '/dashboard/lowest-probability-winner',
    transformData: (raw) => {
      const data = raw as LowestProbabilityWinner;
      const probability = Math.round(data.probability * 10) / 10;
      return data.playerNames.map((name, i) => ({
        rank: i + 1,
        name,
        count: probability,
        unit: '%',
      }));
    },
  },
  {
    key: 'blockstacking-top-players',
    label: '블록쌓기 최고 기록',
    icon: '🧱',
    endpoint: '/dashboard/blockstacking-top-players',
    transformData: (raw) =>
      (raw as BlockStackingTopPlayer[]).map((p, i) => ({
        rank: i + 1,
        name: p.playerName,
        count: p.maxFloor,
        unit: '층',
      })),
  },
  {
    key: 'game-play-counts',
    label: '게임 인기 순위',
    icon: '🎮',
    endpoint: '/dashboard/game-play-counts',
    transformData: (raw) =>
      (raw as GamePlayCount[]).map((g, i) => ({
        rank: i + 1,
        name: MINI_GAME_NAME_MAP[g.gameType as MiniGameType] || g.gameType,
        count: g.playCount,
      })),
  },
];
