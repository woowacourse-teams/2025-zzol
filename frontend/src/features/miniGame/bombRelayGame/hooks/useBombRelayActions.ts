import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useCallback } from 'react';

export const useBombRelayActions = () => {
  const { send } = useWebSocket();
  const { joinCode, myName } = useIdentifier();

  const sendWord = useCallback(
    (word: string) => {
      send(`/room/${joinCode}/bomb-relay/word`, {
        playerName: myName,
        word,
      });
    },
    [send, joinCode, myName]
  );

  return { sendWord };
};
