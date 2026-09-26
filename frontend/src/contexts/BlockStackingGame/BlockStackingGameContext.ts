import {
  BlockStackingGameState,
  BlockStackingRanking,
  StackedBlock,
} from '@/types/miniGame/blockStackingGame';
import { createContext, useContext } from 'react';

type BlockStackingGameContextType = {
  gameState: BlockStackingGameState;
  rankings: BlockStackingRanking[];
  /** 참가자별로 지금까지 쌓은 블록. 0번은 받침 블록이다 */
  towers: Record<string, StackedBlock[]>;
  isLocalGameOver: boolean;
  setLocalGameOver: () => void;
  endTimeEpochMs: number | null;
  totalTimeSeconds: number;
};

export const BlockStackingGameContext = createContext<BlockStackingGameContextType | null>(null);

export const useBlockStackingGameContext = () => {
  const context = useContext(BlockStackingGameContext);
  if (!context) {
    throw new Error(
      'useBlockStackingGameContext는 BlockStackingGameProvider 안에서 사용해야 합니다.'
    );
  }
  return context;
};
