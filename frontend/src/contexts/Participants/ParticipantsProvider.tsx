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
