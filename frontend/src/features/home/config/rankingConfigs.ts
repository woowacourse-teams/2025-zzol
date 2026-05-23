import type { ComponentType } from 'react';
import type { BlockStackingTopPlayer, RacingGameTopPlayer } from '@/types/dashBoard';
import { BlocksIcon, RacingCarIcon } from '../components/RankingTab/rankingIcons';
import { MOCK_BLOCK_STACKING_TOP_PLAYERS, MOCK_RACING_GAME_TOP_PLAYERS } from './dashboardMock';

export type RankingItem = {
  rank: number;
  name: string;
  count: number;
  unit?: string;
};

export type RankingCategory = {
  key: string;
  label: string;
  icon: ComponentType;
  endpoint: string;
  transformData: (raw: unknown) => RankingItem[];
  mockRaw?: unknown;
};

export const RANKING_CATEGORIES: RankingCategory[] = [
  {
    key: 'blockstacking-top-players',
    label: '블록쌓기 최고 기록',
    icon: BlocksIcon,
    endpoint: '/dashboard/block-stacking-top-players',
    mockRaw: MOCK_BLOCK_STACKING_TOP_PLAYERS,
    transformData: (raw) =>
      (raw as BlockStackingTopPlayer[]).map((p, i) => ({
        rank: i + 1,
        name: p.playerName,
        count: p.maxFloor,
        unit: '층',
      })),
  },
  {
    key: 'racing-game-top-players',
    label: '레이싱게임 최단 기록',
    icon: RacingCarIcon,
    endpoint: '/dashboard/racing-game-top-players',
    mockRaw: MOCK_RACING_GAME_TOP_PLAYERS,
    transformData: (raw) =>
      (raw as RacingGameTopPlayer[]).map((p, i) => ({
        rank: i + 1,
        name: p.playerName,
        count: Math.round(p.bestTime / 10) / 100,
        unit: '초',
      })),
  },
];
