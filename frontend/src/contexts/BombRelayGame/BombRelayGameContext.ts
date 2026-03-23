import {
  BombRelayGameState,
  BombRelayProgressData,
  BombRelayWordResult,
} from '@/types/miniGame/bombRelayGame';
import { createContext, useContext } from 'react';

type BombRelayGameContextType = {
  gameState: BombRelayGameState;
  currentRound: number;
  maxRounds: number;
  currentWord: string;
  currentTurnPlayerName: string;
  eliminatedPlayerName: string | null;
  progressData: BombRelayProgressData;
  lastWordResult: BombRelayWordResult | null;
};

export const BombRelayGameContext = createContext<BombRelayGameContextType | null>(null);

export const useBombRelayGame = () => {
  const context = useContext(BombRelayGameContext);
  if (!context) {
    throw new Error('useBombRelayGame은 BombRelayGameProvider 안에서 사용해야 합니다.');
  }
  return context;
};
