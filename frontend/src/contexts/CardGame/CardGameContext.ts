import {
  CardGameRound,
  CardGameState,
  CardInfo,
  SelectedCardInfo,
} from '@/types/miniGame/cardGame';
import { createContext, useContext } from 'react';

type CardGameContextType = {
  isTransition: boolean;
  currentRound: CardGameRound;
  currentCardGameState: CardGameState;
  cardInfos: CardInfo[];
  selectedCardInfo: SelectedCardInfo;
};

export const CardGameContext = createContext<CardGameContextType | null>(null);

export const useCardGame = () => {
  const context = useContext(CardGameContext);
  if (!context) {
    throw new Error('useCardGame는 CardGameProvider 안에서 사용해야 합니다.');
  }
  return context;
};
