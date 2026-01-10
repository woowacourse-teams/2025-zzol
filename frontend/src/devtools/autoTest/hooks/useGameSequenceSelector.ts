import { useCallback, useMemo, useState } from 'react';
import { MiniGameType, MINI_GAME_NAME_MAP } from '@/types/miniGame/common';

type SelectionState = {
  gameSequence: MiniGameType[];
  availableGames: MiniGameType[];
  isExpanded: boolean;
};

type SelectionActions = {
  toggleExpanded: () => void;
  setExpanded: (isExpanded: boolean) => void;
  toggleGame: (gameType: MiniGameType) => void;
};

export type UseGameSequenceSelectorResult = {
  state: SelectionState;
  actions: SelectionActions;
};

const DEFAULT_GAME_SEQUENCE: MiniGameType[] = ['CARD_GAME'];

export const useGameSequenceSelector = (): UseGameSequenceSelectorResult => {
  const [gameSequence, setGameSequence] = useState<MiniGameType[]>(DEFAULT_GAME_SEQUENCE);
  const [isGameSelectionExpanded, setGameSelectionExpanded] = useState<boolean>(false);

  const availableGames = useMemo(() => {
    return Object.keys(MINI_GAME_NAME_MAP) as MiniGameType[];
  }, []);

  const toggleGameSelectionExpanded = useCallback(() => {
    setGameSelectionExpanded((prev) => !prev);
  }, []);

  const handleGameToggle = useCallback((gameType: MiniGameType) => {
    setGameSequence((prev) => {
      if (prev.includes(gameType)) {
        return prev.filter((game) => game !== gameType);
      }
      return [...prev, gameType];
    });
  }, []);

  return {
    state: {
      gameSequence,
      availableGames,
      isExpanded: isGameSelectionExpanded,
    },
    actions: {
      toggleExpanded: toggleGameSelectionExpanded,
      setExpanded: setGameSelectionExpanded,
      toggleGame: handleGameToggle,
    },
  };
};
