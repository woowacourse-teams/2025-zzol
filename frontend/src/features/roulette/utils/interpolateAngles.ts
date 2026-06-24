import { convertProbabilitiesToAngles } from './convertProbabilitiesToAngles';
import { PlayerProbability, RouletteSector } from '@/types/roulette';

type Props = {
  from: PlayerProbability[];
  to: PlayerProbability[];
  progress: number;
};

export const interpolateAngles = ({ from, to, progress }: Props): RouletteSector[] => {
  const fromAngles = convertProbabilitiesToAngles(from);
  const toAngles = convertProbabilitiesToAngles(to);

  return fromAngles.map((fromPlayer, i) => {
    const toPlayer = toAngles[i];

    return {
      playerName: fromPlayer.playerName,
      startAngle: fromPlayer.startAngle + (toPlayer.startAngle - fromPlayer.startAngle) * progress,
      endAngle: fromPlayer.endAngle + (toPlayer.endAngle - fromPlayer.endAngle) * progress,
      playerColor: fromPlayer.playerColor,
    };
  });
};
