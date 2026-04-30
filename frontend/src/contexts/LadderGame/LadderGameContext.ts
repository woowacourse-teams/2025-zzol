import { LadderGameState, LadderLine, Pole } from '@/types/miniGame/ladderGame';
import { createContext, useContext } from 'react';

type LadderGameContextType = {
  gameState: LadderGameState;
  poles: Pole[];
  bottomRanks: Record<string, number>;
  lines: LadderLine[];
  ghostSegmentIndex: number | null;
  endTimeEpochMs: number | null;
  rankings: Record<string, number> | null;
  animationDurationMs: number | null;
  drawLine: (segmentIndex: number) => void;
};

export const LadderGameContext = createContext<LadderGameContextType | null>(null);

export const useLadderGameContext = () => {
  const context = useContext(LadderGameContext);
  if (!context) {
    throw new Error('useLadderGameContext는 LadderGameProvider 안에서 사용해야 합니다.');
  }
  return context;
};
