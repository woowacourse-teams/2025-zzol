export type SpeedTouchGameState = 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'DONE';

export type PlayerProgress = {
  playerName: string;
  currentNumber: number;
  finished: boolean;
};

export type SpeedTouchProgressData = {
  players: PlayerProgress[];
};
