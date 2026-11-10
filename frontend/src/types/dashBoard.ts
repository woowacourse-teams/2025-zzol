export type TopWinner = {
  nickname: string;
  /** 서버가 닉네임을 가린 행은 유저 코드도 비운다(#1906). */
  userCode: string | null;
  winCount: number;
  /** 검열 전 닉네임이라 서버가 원문 대신 고정 문구를 보냈다(#1906). 배포 순서와 무관하게 읽도록 선택값으로 둔다. */
  masked?: boolean;
};

export type LowestProbabilityWinner = {
  players: { nickname: string; userCode: string | null; masked?: boolean }[];
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
