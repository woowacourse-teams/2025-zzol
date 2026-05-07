import useFetch from '@/apis/rest/useFetch';
import { useMockMode } from '@/hooks/useMockMode';
import type { TopWinner, LowestProbabilityWinner, GamePlayCount } from '@/types/dashBoard';
import {
  MOCK_TOP_WINNERS,
  MOCK_LOWEST_PROBABILITY,
  MOCK_GAME_PLAY_COUNTS,
} from '../config/dashboardMock';

const withMockFallback = <T>(data: T[] | undefined, mock: T[], useMock: boolean): T[] => {
  if (useMock) return mock;
  return data ?? [];
};

export const useDashboardData = () => {
  const { mockEnabled } = useMockMode();

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
    topWinners: withMockFallback(topWinners, MOCK_TOP_WINNERS, mockEnabled),
    lowestProbabilityWinner: mockEnabled
      ? MOCK_LOWEST_PROBABILITY
      : (lowestProbabilityWinner ?? null),
    gamePlayCounts: withMockFallback(gamePlayCounts, MOCK_GAME_PLAY_COUNTS, mockEnabled),
    loading: l1 || l2 || l3,
  };
};
