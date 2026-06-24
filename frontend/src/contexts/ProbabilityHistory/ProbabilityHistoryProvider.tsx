import { PropsWithChildren, useCallback, useState } from 'react';
import { ProbabilityHistory, PlayerProbability } from '@/types/roulette';
import { ProbabilityHistoryContext } from './ProbabilityHistoryContext';

const ProbabilityHistoryProvider = ({ children }: PropsWithChildren) => {
  const [probabilityHistoryState, setProbabilityHistoryState] = useState<ProbabilityHistory>({
    prev: [],
    current: [],
  });

  const updateCurrentProbabilities = useCallback((probabilities: PlayerProbability[]) => {
    setProbabilityHistoryState((prevState) => {
      return {
        prev: prevState.current,
        current: probabilities,
      };
    });
  }, []);

  const clearProbabilityHistory = useCallback(() => {
    setProbabilityHistoryState({
      prev: [],
      current: [],
    });
  }, []);

  return (
    <ProbabilityHistoryContext.Provider
      value={{
        probabilityHistory: probabilityHistoryState,
        updateCurrentProbabilities,
        clearProbabilityHistory,
      }}
    >
      {children}
    </ProbabilityHistoryContext.Provider>
  );
};

export default ProbabilityHistoryProvider;
