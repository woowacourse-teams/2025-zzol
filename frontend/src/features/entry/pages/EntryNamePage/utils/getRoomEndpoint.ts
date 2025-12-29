import { JOIN_CODE_LENGTH } from '@/constants/joinCode';
import { PlayerType } from '@/types/player';

export const getRoomEndpoint = (playerType: PlayerType | null, joinCode: string | null): string => {
  validateRoomEndpointParams(playerType, joinCode);

  if (playerType === 'HOST') {
    return '/rooms';
  }

  return `/rooms/${joinCode}`;
};

const validateRoomEndpointParams = (
  playerType: PlayerType | null,
  joinCode: string | null
): void => {
  if (playerType === null) {
    throw new Error('playerType is null');
  }

  if (playerType === 'GUEST') {
    if (joinCode === null) {
      throw new Error('joinCode is null');
    }
    if (joinCode.length !== JOIN_CODE_LENGTH) {
      throw new Error(`joinCode is not ${JOIN_CODE_LENGTH} length`);
    }
  }
};
