import { RacingPlayer } from '@/types/miniGame/racingGame';
import { useMemo } from 'react';

type Props = {
  players: RacingPlayer[];
  myName: string;
};

export const usePlayerData = ({ players, myName }: Props) => {
  const myPlayer = useMemo(
    () => players.find((player) => player.playerName === myName),
    [players, myName]
  );

  const myPosition = myPlayer?.position ?? 0;
  const mySpeed = myPlayer?.speed ?? 0;

  return {
    myPlayer,
    myPosition,
    mySpeed,
  };
};
