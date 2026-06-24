import { RacingPlayer } from '@/types/miniGame/racingGame';
import { useMemo } from 'react';

const MAX_PROGRESS_PERCENTAGE = 100;

type Props = {
  players: RacingPlayer[];
  endDistance: number;
  myName: string;
};

type PlayerProgressData = {
  player: RacingPlayer;
  progress: number;
  isMe: boolean;
  index: number;
};

export const usePlayersProgressData = ({ players, endDistance, myName }: Props) => {
  const playersProgressData = useMemo((): PlayerProgressData[] => {
    return players.map((player, index) => {
      const progress = Math.min(
        (player.position / endDistance) * MAX_PROGRESS_PERCENTAGE,
        MAX_PROGRESS_PERCENTAGE
      );
      const isMe = player.playerName === myName;

      return {
        player,
        progress,
        isMe,
        index,
      };
    });
  }, [players, endDistance, myName]);

  return playersProgressData;
};
