import { useCallback, useMemo } from 'react';
import useMutation from '@/apis/rest/useMutation';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import useToast from '@/components/@common/Toast/useToast';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { getRoomEndpoint } from '../utils/getRoomEndpoint';

type RoomRequest = {
  playerName: string;
};

type RoomResponse = {
  joinCode: string;
};

export const useRoomManagement = () => {
  const navigate = useReplaceNavigate();
  const { startSocket } = useWebSocket();
  const { playerType } = usePlayerType();
  const { joinCode, setJoinCode } = useIdentifier();
  const { showToast } = useToast();

  const endpoint = useMemo(() => getRoomEndpoint(playerType, joinCode), [playerType, joinCode]);

  const createOrJoinRoom = useMutation<RoomResponse, RoomRequest>({
    endpoint,
    method: 'POST',
    onSuccess: (data, variables) => {
      const { joinCode } = data;
      setJoinCode(joinCode);
      startSocket(joinCode, variables.playerName);
      if (joinCode) navigate(`/room/${joinCode}/lobby`);
    },
    errorDisplayMode: 'toast',
  });

  const validatePlayerName = useCallback(
    (playerName: string): boolean => {
      if (!playerName) {
        showToast({
          type: 'error',
          message: '닉네임을 다시 입력해주세요.',
        });
        navigate(-1);
        return false;
      }
      return true;
    },
    [showToast, navigate]
  );

  const proceedToRoom = useCallback(
    async (playerName: string) => {
      if (!validatePlayerName(playerName)) return;
      await createOrJoinRoom.mutate({ playerName });
    },
    [validatePlayerName, createOrJoinRoom]
  );

  return {
    proceedToRoom,
    isLoading: createOrJoinRoom.loading,
    error: createOrJoinRoom.error,
  };
};
