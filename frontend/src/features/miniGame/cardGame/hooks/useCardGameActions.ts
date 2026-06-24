import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useCardGame } from '@/contexts/CardGame/CardGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useCallback } from 'react';

export const useCardGameActions = () => {
  const { myName, joinCode } = useIdentifier();
  const { currentRound, selectedCardInfo } = useCardGame();
  const { send } = useWebSocket();

  const selectCard = useCallback(
    (cardIndex: number) => {
      if (selectedCardInfo[currentRound].isSelected) return;

      send(`/room/${joinCode}/minigame/command`, {
        commandType: 'SELECT_CARD',
        commandRequest: {
          playerName: myName,
          cardIndex,
        },
      });
    },
    [currentRound, selectedCardInfo, send, joinCode, myName]
  );

  return {
    selectCard,
  };
};
