export type LadderGameState = 'DESCRIPTION' | 'PREPARE' | 'DRAWING' | 'RESULT' | 'DONE';

// 서버 LadderLines.MAX_LINES_PER_PLAYER 와 같은 값
export const MAX_LINES_PER_PLAYER = 2;

export type LadderGhost = {
  segmentIndex: number;
  row: number;
};

export type Pole = {
  index: number;
  playerName: string;
  colorIndex?: number;
};

export type LadderLine = {
  playerName: string;
  segmentIndex: number | string;
  row: number;
  colorIndex?: number;
};
