import type { MyRecordsResponse } from '@/types/records';

export const MOCK_MY_RECORDS: MyRecordsResponse = {
  roulette: { winCount: 3, survivalStreak: 5, playCount: 14 },
  minigame: { totalPlayCount: 31, mostPlayed: { type: 'RACING_GAME', playCount: 12 } },
  games: [
    {
      type: 'RACING_GAME',
      playCount: 12,
      best: 12340,
      average: 14020,
      globalAverage: 15800,
      percentile: 23,
      memberCount: 41,
    },
    {
      type: 'BLOCK_STACKING',
      playCount: 7,
      best: 14,
      average: 9,
      globalAverage: 7,
      percentile: 31,
      memberCount: 29,
    },
    {
      type: 'BLIND_TIMER',
      playCount: 3,
      best: 120,
      average: 410,
      globalAverage: 520,
      percentile: 18,
      memberCount: 33,
    },
    {
      type: 'SPEED_TOUCH',
      playCount: 0,
      best: null,
      average: null,
      globalAverage: 9100,
      percentile: null,
      memberCount: 27,
    },
  ],
};
