import useMutation from '@/apis/rest/useMutation';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import useToast from '@/components/@common/Toast/useToast';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { Menu, TemperatureOption } from '@/types/menu';
import { createRoomRequestBody, createUrl } from '../utils/roomApiHelpers';

export type RoomRequest = {
  playerName: string;
  menu: {
    id: number;
    customName: string | null;
    temperature: TemperatureOption;
  };
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

  const createOrJoinRoom = useMutation<RoomResponse, RoomRequest>({
    endpoint: createUrl(playerType, joinCode),
    method: 'POST',
    onSuccess: (data, variables) => {
      const { joinCode } = data;
      setJoinCode(joinCode);
      startSocket(joinCode, variables.playerName);
      if (joinCode) navigate(`/room/${joinCode}/lobby`);
    },
    errorDisplayMode: 'toast',
  });

  const isPlayerNameValid = (playerName: string) => {
    if (!playerName) {
      showToast({
        type: 'error',
        message: '닉네임을 다시 입력해주세요.',
      });
      navigate(-1);
      return false;
    }
    return true;
  };

  const proceedToRoom = async (
    playerName: string,
    selectedMenu: Menu | null,
    customMenuName: string | null,
    selectedTemperature: TemperatureOption
  ) => {
    if (!isPlayerNameValid(playerName)) return;

    const requestBody = createRoomRequestBody(
      playerName,
      selectedMenu,
      customMenuName,
      selectedTemperature
    );

    await createOrJoinRoom.mutate(requestBody);
  };

  return {
    proceedToRoom,
    isLoading: createOrJoinRoom.loading,
    error: createOrJoinRoom.error,
  };
};
