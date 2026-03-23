import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useCallback } from 'react';

export const useSpeedTouchActions = () => {
  const { send } = useWebSocket();
  const { joinCode, myName } = useIdentifier();

  const sendTouch = useCallback(
    (touchedNumber: number) => {
      send(`/room/${joinCode}/speed-touch/touch`, {
        playerName: myName,
        touchedNumber,
      });
    },
    [send, joinCode, myName]
  );

  return { sendTouch };
};
