import { SpeedTouchGameState, SpeedTouchProgressData } from '@/types/miniGame/speedTouchGame';
import { createContext, useContext } from 'react';

type SpeedTouchGameContextType = {
  gameState: SpeedTouchGameState;
  progressData: SpeedTouchProgressData;
};

export const SpeedTouchGameContext = createContext<SpeedTouchGameContextType | null>(null);

export const useSpeedTouchGame = () => {
  const context = useContext(SpeedTouchGameContext);
  if (!context) {
    throw new Error('useSpeedTouchGame은 SpeedTouchGameProvider 안에서 사용해야 합니다.');
  }
  return context;
};
