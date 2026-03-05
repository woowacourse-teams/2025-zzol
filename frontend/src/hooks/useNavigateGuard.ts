import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { getIsRecovering } from '@/apis/websocket/contexts/WebSocketProvider';
import { useReplaceNavigate } from './useReplaceNavigate';

export const useNavigationGuard = () => {
  const location = useLocation();
  const navigate = useReplaceNavigate();

  useEffect(() => {
    // 복구 중이면 검증 스킵
    if (getIsRecovering()) {
      console.log('복구 중 - 네비게이션 가드 건너뜀');
      return;
    }

    if (!location.state?.fromInternal) {
      console.log('직접 URL 접근 감지 - 홈으로 리디렉션');
      navigate('/');
    }
  }, [location, navigate]);
};
