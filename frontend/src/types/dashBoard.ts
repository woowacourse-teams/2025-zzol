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
  masked?: boolean;
  maxFloor: number;
};

export type RacingGameTopPlayer = {
  playerName: string;
  masked?: boolean;
  bestTime: number;
};

export type BlindTimerTopPlayer = {
  playerName: string;
  masked?: boolean;
  bestErrorMillis: number;
};

export type SpeedTouchTopPlayer = {
  playerName: string;
  masked?: boolean;
  bestTime: number;
};

export type WormGameTopPlayer = {
  playerName: string;
  masked?: boolean;
  /** 최장 생존 시간(ms) */
  bestSurvivalMillis: number;
};
