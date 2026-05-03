import type { ComponentType } from 'react';
import type {
  TopWinner,
  GamePlayCount,
  LowestProbabilityWinner,
  BlockStackingTopPlayer,
  RacingGameTopPlayer,
} from '@/types/dashBoard';
import { MINI_GAME_NAME_MAP, type MiniGameType } from '@/types/miniGame/common';
import {
  TrophyIcon,
  SkullIcon,
  BlocksIcon,
  GamepadIcon,
  RacingCarIcon,
} from '../components/RankingTab/rankingIcons';

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
};

export const RANKING_CATEGORIES: RankingCategory[] = [
  {
    key: 'top-winners',
    label: '이번 달 당첨 랭킹',
    icon: TrophyIcon,
    endpoint: '/dashboard/top-winners',
    transformData: (raw) =>
      (raw as TopWinner[]).map((w, i) => ({
        rank: i + 1,
        name: w.nickname,
        count: w.winCount,
      })),
  },
  {
    key: 'lowest-probability',
    label: '최저 확률 당첨자',
    icon: SkullIcon,
    endpoint: '/dashboard/lowest-probability-winner',
    transformData: (raw) => {
      const data = raw as LowestProbabilityWinner;
      if (!data?.players?.length) return [];
      const probability = Math.round(data.probability * 10) / 10;
      return data.players.map((p, i) => ({
        rank: i + 1,
        name: `${p.nickname} (${p.userCode})`,
        count: probability,
        unit: '%',
      }));
    },
  },
  {
    key: 'blockstacking-top-players',
    label: '블록쌓기 최고 기록',
    icon: BlocksIcon,
    endpoint: '/dashboard/block-stacking-top-players',
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
    transformData: (raw) =>
      (raw as RacingGameTopPlayer[]).map((p, i) => ({
        rank: i + 1,
        name: p.playerName,
        count: Math.round(p.bestTime / 10) / 100,
        unit: '초',
      })),
  },
  {
    key: 'game-play-counts',
    label: '게임 인기 순위',
    icon: GamepadIcon,
    endpoint: '/dashboard/game-play-counts',
    transformData: (raw) =>
      (raw as GamePlayCount[]).map((g, i) => ({
        rank: i + 1,
        name: MINI_GAME_NAME_MAP[g.gameType as MiniGameType] || g.gameType,
        count: g.playCount,
      })),
  },
];
