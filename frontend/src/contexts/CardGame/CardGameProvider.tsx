import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { PropsWithChildren } from 'react';
import { useIdentifier } from '../Identifier/IdentifierContext';
import { CardGameContext } from './CardGameContext';
import { useCardGameState } from './hooks/useCardGameState';
import { useSelectedCard } from './hooks/useSelectedCard';
import { useCardGameHandlers } from './hooks/useCardGameHandlers';

const CardGameProvider = ({ children }: PropsWithChildren) => {
  const { joinCode, myName } = useIdentifier();

  const { dispatch, isTransition, currentRound, currentCardGameState, cardInfos } =
    useCardGameState();

  const { selectedCardInfo, updateSelectedCardInfo } = useSelectedCard(myName);

  const { handleCardGameState } = useCardGameHandlers(dispatch, {
    updateSelectedCardInfo,
  });

  useWebSocketSubscription(`/room/${joinCode}/gameState`, handleCardGameState);

  return (
    <CardGameContext.Provider
      value={{
        isTransition,
        currentRound,
        currentCardGameState,
        cardInfos,
        selectedCardInfo,
      }}
    >
      {children}
    </CardGameContext.Provider>
  );
};

export default CardGameProvider;
