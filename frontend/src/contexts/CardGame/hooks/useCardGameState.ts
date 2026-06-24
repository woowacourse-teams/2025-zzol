import { useReducer } from 'react';
import { cardGameReducer, initialState } from '../reducer/cardGameReducer';

export const useCardGameState = () => {
  const [state, dispatch] = useReducer(cardGameReducer, initialState);

  return {
    dispatch,
    isTransition: state.isTransition,
    currentRound: state.currentRound,
    currentCardGameState: state.currentCardGameState,
    cardInfos: state.cardInfos,
  };
};
