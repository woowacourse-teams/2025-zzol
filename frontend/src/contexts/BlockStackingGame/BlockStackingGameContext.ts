import { BlockStackingGameState } from '@/types/miniGame/blockStackingGame';
import { createContext, useContext } from 'react';

type BlockStackingRanking = {
  name: string;
  floor: number;
};

type BlockStackingGameContextType = {
  gameState: BlockStackingGameState;
  rankings: BlockStackingRanking[];
  isLocalGameOver: boolean;
  setLocalGameOver: () => void;
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
