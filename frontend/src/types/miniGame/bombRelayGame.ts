export type BombRelayGameState = 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'ROUND_RESULT' | 'DONE';

export type BombRelayStateData = {
  state: BombRelayGameState;
  currentRound: number;
  maxRounds: number;
  currentWord: string;
  currentTurnPlayerName: string;
  eliminatedPlayerName: string | null;
};

export type BombRelayPlayerProgress = {
  playerName: string;
  eliminated: boolean;
  eliminatedRound: number;
};

export type BombRelayProgressData = {
  currentWord: string;
  currentTurnPlayerName: string;
  currentRound: number;
  players: BombRelayPlayerProgress[];
};

export type BombRelayWordResult = {
  playerName: string;
  word: string;
  accepted: boolean;
  rejectReason: string | null;
};
