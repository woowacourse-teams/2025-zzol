import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import type { ReactNode } from 'react';
import { api, ApiError } from '@/api/client';
import type { AdminMe, AdminToken } from '@/api/types';
import { clearToken, readToken, saveToken, subscribeToken } from '@/auth/tokenStore';
import { disableGoogleAutoSelect } from '@/auth/google';

type AuthState =
  /** 저장된 토큰이 아직 유효한지 확인하는 중. 이 동안 화면을 그리면 깜빡인다. */
  { status: 'loading' } | { status: 'authenticated'; email: string } | { status: 'anonymous' };

type AuthContextValue = AuthState & {
  /** 구글 ID 토큰을 관리자 토큰으로 교환한다. */
  loginWithGoogle: (idToken: string) => Promise<void>;
  /** local 프로필 전용 우회 경로. 구글 클라이언트 없이 개발할 때 쓴다. */
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  /*
   * 토큰이 있는지는 첫 렌더 전에 이미 알 수 있다. 그래서 초기값에서 갈라 둔다.
   * 무조건 loading 으로 시작하고 이펙트에서 anonymous 로 내리면, 토큰이 없는
   * 사람에게 로딩 화면이 한 프레임 번쩍이고 렌더가 한 번 더 돈다.
   */
  const [state, setState] = useState<AuthState>(() =>
    readToken() ? { status: 'loading' } : { status: 'anonymous' },
  );

  /**
   * 로그인 교환 중에는 구독이 끼어들지 않게 한다.
   *
   * <p>토큰을 저장하는 순간 구독이 돌면서 "토큰이 새로 생겼으니 확인하자"로 들어가는데,
   * 교환 코드가 이미 그 확인을 하고 있다. 막지 않으면 로그인할 때마다 {@code /auth/me}
   * 가 두 번 나간다.
   */
  const exchanging = useRef(false);

  /**
   * 저장된 토큰이 아직 살아 있는지 서버에 묻는다.
   *
   * <p>새로고침할 때도 돌고, 다른 탭에서 로그인해 토큰이 생겼을 때도 돈다. 만료된 토큰으로
   * 화면을 그리면 첫 조회가 401 로 떨어지면서 빈 화면이 먼저 보인다.
   */
  useEffect(() => {
    if (state.status !== 'loading' || exchanging.current) {
      return;
    }

    // 토큰이 없는 경우를 따로 가르지 않는다. 여기 올 때는 토큰이 있었고, 그사이 사라졌다면
    // 헤더 없이 나간 요청을 서버가 401 로 돌려보내 아래 catch 가 같은 자리로 데려온다.
    // 이펙트 안에서 setState 를 곧바로 부르면 렌더가 한 번 더 돌기도 한다.
    let cancelled = false;
    api
      .get<AdminMe>('/auth/me')
      .then((me) => {
        if (!cancelled) setState({ status: 'authenticated', email: me.email });
      })
      .catch(() => {
        // client 가 401 에서 이미 토큰을 지운다. 여기서는 상태만 맞춘다.
        if (!cancelled) setState({ status: 'anonymous' });
      });

    return () => {
      cancelled = true;
    };
  }, [state.status]);

  /**
   * 토큰이 바뀌면 화면 상태를 따라 내린다.
   *
   * <p>이것이 없으면 <b>토큰이 만료돼도 화면은 로그인된 채로 남는다.</b> 401 을 받은
   * api client 가 토큰을 지우지만 그 사실을 아무도 전하지 않아서, 조회마다 401 이 반복되고
   * 사용자는 새로고침해도 안 풀리는 상태에 갇힌다. 직접 주소창에 /login 을 쳐야 빠져나왔다.
   *
   * <p>anonymous 로 내려가면 라우터가 로그인 화면으로 보낸다. 다른 탭에서 로그아웃해도
   * 같은 경로로 이 탭이 따라 내려가고, 다른 탭에서 다시 로그인하면 확인을 거쳐 돌아온다.
   *
   * <p>여기서 {@code logout()} 을 부르지 않는다. 그쪽은 구글 자동 선택까지 끄는데, 그건
   * <b>사람이 로그아웃 버튼을 눌렀을 때</b> 계정 선택 화면을 다시 띄우려는 것이다. 만료는
   * 사람이 한 일이 아니라 시간이 한 일이라, 같은 계정으로 바로 다시 들어갈 수 있어야 한다.
   */
  useEffect(
    () =>
      subscribeToken((token) => {
        if (exchanging.current) {
          return;
        }
        setState((current) => {
          if (!token) {
            return current.status === 'anonymous' ? current : { status: 'anonymous' };
          }
          // 다른 탭에서 로그인했다. 이 토큰이 쓸 수 있는 것인지는 서버가 답한다.
          return current.status === 'anonymous' ? { status: 'loading' } : current;
        });
      }),
    [],
  );

  const exchange = useCallback(async (issue: () => Promise<AdminToken>) => {
    exchanging.current = true;
    try {
      const { accessToken } = await issue();
      saveToken(accessToken);
      const me = await api.get<AdminMe>('/auth/me');
      setState({ status: 'authenticated', email: me.email });
    } finally {
      exchanging.current = false;
    }
  }, []);

  const loginWithGoogle = useCallback(
    (idToken: string) => exchange(() => api.post<AdminToken>('/auth/login', { idToken })),
    [exchange],
  );

  const logout = useCallback(() => {
    clearToken();
    // 다음 로그인에서 계정 선택 화면이 다시 뜨게 한다. 이것을 안 하면 방금 로그아웃한
    // 계정으로 곧바로 다시 들어가진다.
    disableGoogleAutoSelect();
    setState({ status: 'anonymous' });
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, loginWithGoogle, logout }),
    [state, loginWithGoogle, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth 는 AuthProvider 안에서만 쓸 수 있습니다.');
  }
  return context;
}

/** 로그인 실패 메시지를 사람이 읽을 문장으로. */
export function toLoginErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.isForbidden) {
      return '관리자 허용목록에 없는 계정입니다.';
    }
    return error.message;
  }
  return '로그인에 실패했습니다. 잠시 후 다시 시도해주세요.';
}
