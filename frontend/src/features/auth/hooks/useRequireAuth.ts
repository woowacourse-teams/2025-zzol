import { useCallback } from 'react';
import type { ReactNode } from 'react';
import useModal from '@/components/@common/Modal/useModal';
import { useAuth } from '../contexts/AuthContext';

// 로그인이 필요한 액션에서 미로그인 시 LoginSheet를 열고, 로그인 상태면 즉시 action 실행
export const useRequireAuth = () => {
  const { isAuthenticated } = useAuth();
  const { openModal } = useModal();

  const requireAuth = useCallback(
    (action: () => void, loginSheetContent: ReactNode) => {
      if (isAuthenticated) {
        action();
        return;
      }
      openModal(loginSheetContent);
    },
    [isAuthenticated, openModal]
  );

  return { requireAuth, isAuthenticated };
};
