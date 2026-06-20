import type {
  TopWinner,
  LowestProbabilityWinner,
  GamePlayCount,
  BlockStackingTopPlayer,
  RacingGameTopPlayer,
  BlindTimerTopPlayer,
  SpeedTouchTopPlayer,
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

export const MOCK_BLIND_TIMER_TOP_PLAYERS: BlindTimerTopPlayer[] = [
  { playerName: '체감시계', bestErrorMillis: 80 },
  { playerName: '뇌피셜장인', bestErrorMillis: 150 },
  { playerName: '느낌적인느낌', bestErrorMillis: 230 },
  { playerName: '시간감각', bestErrorMillis: 360 },
  { playerName: '대충맞춤', bestErrorMillis: 470 },
];

export const MOCK_SPEED_TOUCH_TOP_PLAYERS: SpeedTouchTopPlayer[] = [
  { playerName: '터치왕', bestTime: 18230 },
  { playerName: '손가락폭풍', bestTime: 19840 },
  { playerName: '0.1초컷', bestTime: 21470 },
  { playerName: '눈보다손', bestTime: 23910 },
  { playerName: '연타고수', bestTime: 26350 },
];
