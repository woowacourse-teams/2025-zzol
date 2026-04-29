export type LadderGameState = 'DESCRIPTION' | 'PREPARE' | 'DRAWING' | 'RESULT' | 'DONE';

export type Pole = {
  index: number;
  playerName: string;
};

export type LadderLine = {
  playerName: string;
  segmentIndex: number;
  row: number;
};
