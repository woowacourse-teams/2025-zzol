import { useState } from 'react';
import useFetch from '@/apis/rest/useFetch';
import { useAuth } from '@/features/auth/contexts/AuthContext';
import { useMockMode } from '@/hooks/useMockMode';
import type { MyRecordsResponse } from '@/types/records';
import { MOCK_MY_RECORDS } from '../config/recordsMock';

const ENDPOINT = '/users/me/records' as const;

/**
 * 로그인 회원의 룰렛·미니게임 누적 기록. mock 모드면 요청 없이 샘플 응답을 돌려준다.
 * 'text' 모드는 useFetch 가 error 를 비우므로 실패 여부는 onError 로 따로 잡는다.
 */
export const useMyRecords = () => {
  const { isAuthenticated } = useAuth();
  const { mockEnabled } = useMockMode();
  const [failed, setFailed] = useState(false);

  const { data, loading } = useFetch<MyRecordsResponse>({
    endpoint: ENDPOINT,
    enabled: isAuthenticated && !mockEnabled,
    errorDisplayMode: 'text',
    onError: () => setFailed(true),
  });

  if (mockEnabled) return { data: MOCK_MY_RECORDS, loading: false, failed: false };
  return { data, loading: loading || (!data && !failed), failed };
};
