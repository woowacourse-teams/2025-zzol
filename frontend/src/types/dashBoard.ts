export type TopWinner = {
  playerName: string;
  winCount: number;
};

export type LowestProbabilityWinner = {
  playerNames: string[];
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
