export type TopWinner = {
  nickname: string;
  userCode: string;
  winCount: number;
};

export type LowestProbabilityWinner = {
  players: { nickname: string; userCode: string }[];
  probability: number;
};

export type GamePlayCount = {
  gameType: string;
  playCount: number;
};

export type BlockStackingTopPlayer = {
  playerName: string;
  maxFloor: number;
};

export type RacingGameTopPlayer = {
  playerName: string;
  bestTime: number;
};

export type BlindTimerTopPlayer = {
  playerName: string;
  bestErrorMillis: number;
};

export type SpeedTouchTopPlayer = {
  playerName: string;
  bestTime: number;
};
