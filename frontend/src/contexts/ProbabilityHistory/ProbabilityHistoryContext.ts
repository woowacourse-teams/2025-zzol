import { ProbabilityHistory, PlayerProbability } from '@/types/roulette';
import { createContext, useContext } from 'react';

export type ProbabilityHistoryContextType = {
  probabilityHistory: ProbabilityHistory;
  updateCurrentProbabilities: (probabilities: PlayerProbability[]) => void;
  clearProbabilityHistory: () => void;
};

export const ProbabilityHistoryContext = createContext<ProbabilityHistoryContextType | null>(null);

export const useProbabilityHistory = () => {
  const context = useContext(ProbabilityHistoryContext);
  if (!context) {
    throw new Error('useProbabilityHistory는 ProbabilityHistoryProvider 안에서 사용해야 합니다.');
  }
  return context;
};
