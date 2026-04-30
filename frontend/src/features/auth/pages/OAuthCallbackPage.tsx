import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import useToast from '@/components/@common/Toast/useToast';
import { useAuth } from '../contexts/AuthContext';
import { tokenStore } from '../tokens';

// 백엔드 redirect 후 도착 페이지 — URL params에서 토큰 추출 → 세션 복원 → 홈으로 이동
const OAuthCallbackPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { refreshUser } = useAuth();
  const handledRef = useRef(false);

  useEffect(() => {
    if (handledRef.current) return;
    handledRef.current = true;

    const handle = async () => {
      const accessToken = searchParams.get('accessToken');
      const refreshToken = searchParams.get('refreshToken');
      const error = searchParams.get('error');

      if (error) {
        showToast({ message: '로그인 중 오류가 발생했습니다.', type: 'error' });
        navigate('/', { replace: true });
        return;
      }

      if (!accessToken || !refreshToken) {
        showToast({ message: '인증 정보를 찾을 수 없습니다.', type: 'error' });
        navigate('/', { replace: true });
        return;
      }

      try {
        tokenStore.setTokens({ accessToken, refreshToken });
        await refreshUser();
        navigate('/', { replace: true });
      } catch {
        showToast({ message: '로그인 처리 중 오류가 발생했습니다.', type: 'error' });
        navigate('/', { replace: true });
      }
    };

    handle();
  }, [searchParams, navigate, showToast, refreshUser]);

  return null;
};

export default OAuthCallbackPage;
