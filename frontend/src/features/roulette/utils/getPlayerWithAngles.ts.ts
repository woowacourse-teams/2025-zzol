import { PlayerProbability } from '@/types/roulette';

type PlayerWithAngle = {
  startAngle: number;
  endAngle: number;
} & PlayerProbability;

export const getPlayersWithAngles = (
  playerProbabilities: PlayerProbability[],
  totalProbability: number
): PlayerWithAngle[] => {
  let currentAngle = 0;
  return playerProbabilities.map((player) => {
    const angle = (player.probability / totalProbability) * 360;
    const startAngle = currentAngle;
    const endAngle = currentAngle + angle;
    currentAngle = endAngle;
    return { ...player, startAngle, endAngle };
  });
};
