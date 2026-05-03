import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import useToast from '@/components/@common/Toast/useToast';
import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';
import { useAuth } from '../contexts/AuthContext';
import { tokenStore } from '../tokens';
import { authApi } from '../api/authApi';

// 백엔드 redirect 후 도착 페이지 — code로 토큰 교환 → 세션 복원 → 홈으로 이동
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
      const code = searchParams.get('code');
      const error = searchParams.get('error');

      if (error) {
        showToast({ message: '로그인 중 오류가 발생했습니다.', type: 'error' });
        navigate('/', { replace: true });
        return;
      }

      if (!code) {
        showToast({ message: '인증 정보를 찾을 수 없습니다.', type: 'error' });
        navigate('/', { replace: true });
        return;
      }

      try {
        const result = await authApi.token(code);
        if (result.isNewUser) {
          tokenStore.clearTokens();
          storageManager.setItem(STORAGE_KEYS.TEMP_TOKEN, result.accessToken, 'sessionStorage');
          navigate('/auth/terms', { replace: true });
        } else {
          tokenStore.setTokens({ accessToken: result.accessToken });
          await refreshUser();
          navigate('/', { replace: true });
        }
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
