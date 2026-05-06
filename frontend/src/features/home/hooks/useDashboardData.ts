import useFetch from '@/apis/rest/useFetch';
import type { TopWinner, LowestProbabilityWinner, GamePlayCount } from '@/types/dashBoard';
import {
  MOCK_TOP_WINNERS,
  MOCK_LOWEST_PROBABILITY,
  MOCK_GAME_PLAY_COUNTS,
} from '../config/dashboardMock';

const isDev = process.env.NODE_ENV === 'development';

const withDevFallback = <T>(data: T[] | undefined, mock: T[]): T[] => {
  if (data?.length) return data;
  return isDev ? mock : [];
};

export const useDashboardData = () => {
  const { data: topWinners } = useFetch<TopWinner[]>({ endpoint: '/dashboard/top-winners' });
  const { data: lowestProbabilityWinner } = useFetch<LowestProbabilityWinner>({
    endpoint: '/dashboard/lowest-probability-winner',
  });
  const { data: gamePlayCounts } = useFetch<GamePlayCount[]>({
    endpoint: '/dashboard/game-play-counts',
  });

  return {
    topWinners: withDevFallback(topWinners, MOCK_TOP_WINNERS),
    lowestProbabilityWinner: lowestProbabilityWinner ?? (isDev ? MOCK_LOWEST_PROBABILITY : null),
    gamePlayCounts: withDevFallback(gamePlayCounts, MOCK_GAME_PLAY_COUNTS),
  };
};
