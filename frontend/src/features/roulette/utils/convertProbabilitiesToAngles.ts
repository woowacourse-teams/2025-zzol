import { PlayerProbability } from '@/types/roulette';
import { getPlayersWithAngles } from './index';

export const convertProbabilitiesToAngles = (playerProbabilities: PlayerProbability[]) => {
  const totalProbability = playerProbabilities.reduce((sum, player) => sum + player.probability, 0);
  return getPlayersWithAngles(playerProbabilities, totalProbability);
};
