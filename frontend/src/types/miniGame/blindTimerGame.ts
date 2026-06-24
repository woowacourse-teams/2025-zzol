export type BlindTimerGameState = 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'DONE';

export type BlindTimerStateData = {
  state: BlindTimerGameState;
  targetTimeMillis: number;
  blindDelayMillis: number;
};

export type BlindTimerPlayerProgress = {
  playerName: string;
  stopped: boolean;
  timedOut: boolean;
};

export type BlindTimerProgressData = {
  players: BlindTimerPlayerProgress[];
};
