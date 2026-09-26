export type BlockStackingGameState = 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'DONE';

/** 서버 progress 브로드캐스트의 참가자 한 명. top* 은 아직 한 층도 못 쌓았으면 null */
export type BlockStackingRanking = {
  name: string;
  floor: number;
  failed: boolean;
  topX: number | null;
  topWidth: number | null;
};

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
