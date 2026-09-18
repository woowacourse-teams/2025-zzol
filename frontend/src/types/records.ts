import type { MiniGameType } from '@/types/miniGame/common';

/** 기록이 집계되는 게임. 서버는 항상 이 4종을 이 순서로 내려준다. */
export type RecordGameType = Extract<
  MiniGameType,
  'RACING_GAME' | 'BLOCK_STACKING' | 'BLIND_TIMER' | 'SPEED_TOUCH'
>;

/**
 * best·average 는 저장 단위 그대로다.
 * RACING_GAME·SPEED_TOUCH 는 완주 ms, BLIND_TIMER 는 오차 ms, BLOCK_STACKING 은 층수.
 * 완주한 판이 없으면 둘 다 null 이다.
 */
export type GameRecord = {
  type: RecordGameType;
  playCount: number;
  best: number | null;
  average: number | null;
};

export type MyRecordsResponse = {
  roulette: {
    winCount: number;
    survivalStreak: number;
    playCount: number;
  };
  minigame: {
    totalPlayCount: number;
    /** totalPlayCount 가 0 이면 null */
    mostPlayed: { type: MiniGameType; playCount: number } | null;
  };
  games: GameRecord[];
};
