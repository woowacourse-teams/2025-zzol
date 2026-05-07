import type {
  TopWinner,
  LowestProbabilityWinner,
  GamePlayCount,
  BlockStackingTopPlayer,
  RacingGameTopPlayer,
} from '@/types/dashBoard';

export const MOCK_TOP_WINNERS: TopWinner[] = [
  { nickname: '최강당첨', userCode: 'A1B2', winCount: 12 },
  { nickname: '럭키가이', userCode: 'C3D4', winCount: 9 },
  { nickname: '행운아', userCode: 'E5F6', winCount: 7 },
  { nickname: '기적의손', userCode: 'G7H8', winCount: 5 },
  { nickname: '쫄왕', userCode: 'I9J0', winCount: 3 },
];

export const MOCK_LOWEST_PROBABILITY: LowestProbabilityWinner = {
  players: [{ nickname: '기적체험', userCode: 'K1L2' }],
  probability: 1.8,
};

export const MOCK_GAME_PLAY_COUNTS: GamePlayCount[] = [
  { gameType: 'RACING_GAME', playCount: 1523 },
  { gameType: 'BLOCK_STACKING', playCount: 1201 },
  { gameType: 'CARD_GAME', playCount: 987 },
  { gameType: 'SPEED_TOUCH', playCount: 743 },
  { gameType: 'BLIND_TIMER', playCount: 512 },
  { gameType: 'LADDER_GAME', playCount: 334 },
];

export const MOCK_BLOCK_STACKING_TOP_PLAYERS: BlockStackingTopPlayer[] = [
  { playerName: '하늘까지', maxFloor: 47 },
  { playerName: '쌓기왕', maxFloor: 41 },
  { playerName: '블록신', maxFloor: 38 },
  { playerName: '중력무시', maxFloor: 33 },
  { playerName: '탑빌더', maxFloor: 29 },
];

export const MOCK_RACING_GAME_TOP_PLAYERS: RacingGameTopPlayer[] = [
  { playerName: '번개손', bestTime: 320 },
  { playerName: '순간이동', bestTime: 347 },
  { playerName: '광속', bestTime: 381 },
  { playerName: '빠름주의', bestTime: 412 },
  { playerName: '달려라', bestTime: 443 },
];
