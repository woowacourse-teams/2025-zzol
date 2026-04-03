export type BlockStackingGameState = 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'DONE';

export type StackedBlock = {
  x: number;
  width: number;
};

export type FallingPiece = {
  x: number;
  y: number;
  width: number;
  vy: number;
  opacity: number;
  color: string;
};

export type CurrentBlock = {
  x: number;
  width: number;
  direction: 1 | -1;
};
