import type { ComponentType } from 'react';
import type {
  BlockStackingTopPlayer,
  RacingGameTopPlayer,
  BlindTimerTopPlayer,
  SpeedTouchTopPlayer,
  WormGameTopPlayer,
} from '@/types/dashBoard';
import {
  BlocksIcon,
  RacingCarIcon,
  StopwatchIcon,
  TouchIcon,
  WormIcon,
} from '../components/RankingTab/rankingIcons';
import {
  MOCK_BLOCK_STACKING_TOP_PLAYERS,
  MOCK_RACING_GAME_TOP_PLAYERS,
  MOCK_BLIND_TIMER_TOP_PLAYERS,
  MOCK_SPEED_TOUCH_TOP_PLAYERS,
  MOCK_WORM_GAME_TOP_PLAYERS,
} from './dashboardMock';
import { millisToSeconds } from '../utils/formatRecord';

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
        count: millisToSeconds(p.bestTime),
        unit: '초',
      })),
  },
  {
    key: 'blind-timer-top-players',
    label: '뇌피셜 초시계 최소 오차',
    icon: StopwatchIcon,
    endpoint: '/dashboard/blind-timer-top-players',
    mockRaw: MOCK_BLIND_TIMER_TOP_PLAYERS,
    transformData: (raw) =>
      (raw as BlindTimerTopPlayer[]).map((p, i) => ({
        rank: i + 1,
        name: p.playerName,
        count: millisToSeconds(p.bestErrorMillis),
        unit: '초',
      })),
  },
  {
    key: 'speed-touch-top-players',
    label: '스피드터치 최단 기록',
    icon: TouchIcon,
    endpoint: '/dashboard/speed-touch-top-players',
    mockRaw: MOCK_SPEED_TOUCH_TOP_PLAYERS,
    transformData: (raw) =>
      (raw as SpeedTouchTopPlayer[]).map((p, i) => ({
        rank: i + 1,
        name: p.playerName,
        count: millisToSeconds(p.bestTime),
        unit: '초',
      })),
  },
  {
    key: 'worm-game-top-players',
    label: '지렁이 게임 최장 생존 기록',
    icon: WormIcon,
    endpoint: '/dashboard/worm-game-top-players',
    mockRaw: MOCK_WORM_GAME_TOP_PLAYERS,
    transformData: (raw) =>
      (raw as WormGameTopPlayer[]).map((p, i) => ({
        rank: i + 1,
        name: p.playerName,
        count: millisToSeconds(p.bestSurvivalMillis),
        unit: '초',
      })),
  },
];
