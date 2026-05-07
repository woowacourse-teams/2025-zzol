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
  const { data: topWinners, loading: l1 } = useFetch<TopWinner[]>({
    endpoint: '/dashboard/top-winners',
    errorDisplayMode: 'toast',
  });
  const { data: lowestProbabilityWinner, loading: l2 } = useFetch<LowestProbabilityWinner>({
    endpoint: '/dashboard/lowest-probability-winner',
    errorDisplayMode: 'toast',
  });
  const { data: gamePlayCounts, loading: l3 } = useFetch<GamePlayCount[]>({
    endpoint: '/dashboard/game-play-counts',
    errorDisplayMode: 'toast',
  });

  return {
    topWinners: withDevFallback(topWinners, MOCK_TOP_WINNERS),
    lowestProbabilityWinner: lowestProbabilityWinner?.players?.length
      ? lowestProbabilityWinner
      : isDev
        ? MOCK_LOWEST_PROBABILITY
        : null,
    gamePlayCounts: withDevFallback(gamePlayCounts, MOCK_GAME_PLAY_COUNTS),
    loading: l1 || l2 || l3,
  };
};
