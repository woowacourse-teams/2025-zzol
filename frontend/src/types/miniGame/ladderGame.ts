import type { LadderLineResponse, PoleInfo } from '@/apis/websocket/generated/wsContract';

export type { LadderGameState } from '@/apis/websocket/generated/wsContract';

// 서버 LadderLines.MAX_LINES_PER_PLAYER 와 같은 값
export const MAX_LINES_PER_PLAYER = 2;

export type LadderGhost = {
  segmentIndex: number;
  row: number;
};

export type Pole = PoleInfo;

export type LadderLine = LadderLineResponse;
