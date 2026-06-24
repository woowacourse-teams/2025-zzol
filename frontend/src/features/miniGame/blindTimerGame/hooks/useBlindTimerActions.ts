import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useCallback } from 'react';

export const useBlindTimerActions = () => {
  const { send } = useWebSocket();
  const { joinCode, myName } = useIdentifier();

  const sendStop = useCallback(() => {
    send(`/room/${joinCode}/blind-timer/stop`, {
      playerName: myName,
    });
  }, [send, joinCode, myName]);

  return { sendStop };
};
