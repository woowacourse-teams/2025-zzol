import useFetch from '@/apis/rest/useFetch';
import { useAuth } from '@/features/auth/contexts/AuthContext';
import type { SeasonLeaderboardResponse, SeasonRankResponse } from '@/types/season';

const DEFAULT_LEADERBOARD_LIMIT = 10;

/** 시즌 리더보드 상위 N명. 시즌 미지정 시 서버가 현재 시즌(월)으로 응답한다. */
export const useSeasonLeaderboard = (limit: number = DEFAULT_LEADERBOARD_LIMIT) => {
  return useFetch<SeasonLeaderboardResponse>({
    endpoint: `/settlement/leaderboard?limit=${limit}`,
    errorDisplayMode: 'text',
  });
};

/**
 * 로그인 회원의 이번 시즌 순위. 이번 시즌 정산 이력이 없으면 서버가 404를 주므로
 * data가 비어 있는 것("기록 없음")과 로딩을 구분해 표시한다.
 */
export const useMySeasonRank = () => {
  const { isAuthenticated, user } = useAuth();

  return useFetch<SeasonRankResponse>({
    endpoint: `/settlement/ranks?userCode=${user?.userCode ?? ''}`,
    enabled: isAuthenticated && Boolean(user?.userCode),
    errorDisplayMode: 'text',
  });
};
