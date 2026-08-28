import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useCallback } from 'react';

export const useBlindTimerActions = () => {
  const { send } = useWebSocket();
  const { joinCode } = useIdentifier();

  // playerName은 보내지 않는다 — 서버가 STOMP principal에서 도출한다.
  const sendStop = useCallback(() => {
    send(`/room/${joinCode}/blind-timer/stop`);
  }, [send, joinCode]);

  return { sendStop };
};
