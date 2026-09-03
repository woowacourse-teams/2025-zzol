import { PropsWithChildren, useCallback, useState } from 'react';
import { ParticipantsContext } from './ParticipantsContext';
import { colorList } from '@/constants/color';
import { Player } from '@/types/player';
import { hashIndex } from '@/utils/hashIndex';

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
      // 명단이 복구되기 전에는 못 찾은 이름이 전부 0번 색으로 떨어져 전원이 같은 색이 된다.
      // 이름 해시로 흩어 놓으면 그 구간에도 서로 구분된다.
      return participant?.colorIndex ?? hashIndex(playerName, colorList.length);
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
