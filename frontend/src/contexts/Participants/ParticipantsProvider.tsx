import { PropsWithChildren, useCallback, useState } from 'react';
import { ParticipantsContext } from './ParticipantsContext';
import { Player } from '@/types/player';

export const ParticipantsProvider = ({ children }: PropsWithChildren) => {
  const [participants, setParticipants] = useState<Player[]>([]);

  const checkPlayerReady = useCallback(
    (playerName: string): boolean => {
      const participant = participants.find((participant) => participant.playerName === playerName);
      return participant?.isReady ?? false;
    },
    [participants]
  );
  const isAllReady = participants.every((participant) => participant.isReady);

  const getParticipantColorIndex = useCallback(
    (playerName: string): number => {
      const participant = participants.find((p) => p.playerName === playerName);
      // 색의 원천은 서버 colorIndex 하나다. 명단이 비어 있으면 useRestoreParticipants 가
      // GET /rooms/{joinCode}/players 로 채운다(#1688). 여기서 따로 지어내면 채워지는 순간 색이 갈아엎힌다.
      return participant?.colorIndex ?? 0;
    },
    [participants]
  );

  return (
    <ParticipantsContext.Provider
      value={{
        participants,
        isAllReady,
        setParticipants,
        getParticipantColorIndex,
        checkPlayerReady,
      }}
    >
      {children}
    </ParticipantsContext.Provider>
  );
};
