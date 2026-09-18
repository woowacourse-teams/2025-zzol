import type { MyRecordsResponse } from '@/types/records';

export const MOCK_MY_RECORDS: MyRecordsResponse = {
  roulette: { winCount: 3, survivalStreak: 5, playCount: 14 },
  minigame: { totalPlayCount: 31, mostPlayed: { type: 'RACING_GAME', playCount: 12 } },
  games: [
    { type: 'RACING_GAME', playCount: 12, best: 12340, average: 14020 },
    { type: 'BLOCK_STACKING', playCount: 7, best: 14, average: 9 },
    { type: 'BLIND_TIMER', playCount: 3, best: 120, average: 410 },
    { type: 'SPEED_TOUCH', playCount: 0, best: null, average: null },
  ],
};
