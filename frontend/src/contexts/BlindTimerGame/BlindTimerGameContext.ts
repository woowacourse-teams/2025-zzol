import { BlindTimerGameState, BlindTimerProgressData } from '@/types/miniGame/blindTimerGame';
import { createContext, useContext } from 'react';

type BlindTimerGameContextType = {
  gameState: BlindTimerGameState;
  targetTimeMillis: number;
  blindDelayMillis: number;
  progressData: BlindTimerProgressData;
};

export const BlindTimerGameContext = createContext<BlindTimerGameContextType | null>(null);

export const useBlindTimerGame = () => {
  const context = useContext(BlindTimerGameContext);
  if (!context) {
    throw new Error('useBlindTimerGame은 BlindTimerGameProvider 안에서 사용해야 합니다.');
  }
  return context;
};
