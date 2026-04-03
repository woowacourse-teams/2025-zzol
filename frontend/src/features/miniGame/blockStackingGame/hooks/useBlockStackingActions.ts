import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useCallback } from 'react';

export type BlockStackingProgressPayload = {
  floor: number;
  movingBlockX: number;
  stackTopX: number;
  stackTopWidth: number;
};

export const useBlockStackingActions = () => {
  const { joinCode, myName } = useIdentifier();
  const { send } = useWebSocket();

  const publishProgress = useCallback(
    (payload: BlockStackingProgressPayload) => {
      send(`/room/${joinCode}/block-stacking/progress`, {
        playerName: myName,
        ...payload,
      });
    },
    [joinCode, myName, send]
  );

  return { publishProgress };
};
