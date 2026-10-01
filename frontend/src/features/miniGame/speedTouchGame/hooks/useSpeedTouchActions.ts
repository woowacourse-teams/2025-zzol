import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useCallback } from 'react';

export const useSpeedTouchActions = () => {
  const { send } = useWebSocket();
  const { joinCode } = useIdentifier();

  // playerName은 보내지 않는다 — 서버가 STOMP principal에서 도출한다.
  const sendTouch = useCallback(
    (touchedNumber: number) => {
      send(`/room/${joinCode}/speed-touch/touch`, {
        touchedNumber,
      });
    },
    [send, joinCode]
  );

  return { sendTouch };
};
