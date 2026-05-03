import { PropsWithChildren, useCallback, useEffect, useRef, useState } from 'react';
import * as Sentry from '@sentry/react';
import { authApi } from '../api/authApi';
import { BackendRedirectAuthService } from '../services/BackendRedirectAuthService';
import { MockAuthService } from '../services/MockAuthService';
import { tokenStore } from '../tokens';
import { User } from '../types';
import { AuthContext } from './AuthContext';
import { setAuthInterceptor } from '@/apis/rest/apiRequest';

const isDev = process.env.NODE_ENV !== 'production';

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const authServiceRef = useRef(
    isDev
      ? new MockAuthService(tokenStore, {
          onLogin: (u) => setUser(u),
          onLogout: () => setUser(null),
        })
      : new BackendRedirectAuthService(tokenStore)
  );

  const handleLogout = useCallback(async () => {
    await authServiceRef.current.logout();
    setUser(null);
    Sentry.setUser(null);
  }, []);

  useEffect(() => {
    // interceptor 등록: apiRequest에서 토큰 주입 + 401 시 refresh 위임
    setAuthInterceptor({
      getAccessToken: () => tokenStore.getAccessToken(),
      refresh: async () => {
        await authServiceRef.current.refresh();
        return tokenStore.getAccessToken();
      },
      onExpired: () => {
        tokenStore.clearTokens();
        setUser(null);
        window.dispatchEvent(new CustomEvent('auth:expired'));
      },
    });
  }, []);

  useEffect(() => {
    // 페이지 로드 시 기존 세션 복원
    const bootstrap = async () => {
      const hasToken = Boolean(tokenStore.getAccessToken());
      // CookieTokenStore는 메모리 초기화라 항상 null → cookie 존재 여부 판단 불가
      // 항상 /auth/me 시도 → 실패 시 조용히 익명 상태 유지
      try {
        if (hasToken || !isDev) {
          const me = await authApi.me();
          setUser(me);
          Sentry.setUser({ id: me.userCode, username: me.provider });
        }
      } catch {
        // 세션 없음 → 익명 상태 유지
      } finally {
        setIsLoading(false);
      }
    };

    bootstrap();
  }, []);

  const login = useCallback((provider: Parameters<typeof authServiceRef.current.startOAuth>[0]) => {
    authServiceRef.current.startOAuth(provider);
  }, []);

  const refreshUser = useCallback(async () => {
    try {
      const me = await authApi.me();
      setUser(me);
      Sentry.setUser({ id: me.userCode, username: me.provider });
    } catch {
      setUser(null);
    }
  }, []);

  const updateNickname = useCallback(async (nickname: string) => {
    const updated = await authApi.updateNickname(nickname);
    setUser(updated);
    Sentry.setUser({ id: updated.userCode, username: updated.provider });
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: user !== null,
        isLoading,
        login,
        logout: handleLogout,
        refreshUser,
        updateNickname,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
