import { act, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from '@/auth/AuthProvider';
import { clearToken, saveToken } from '@/auth/tokenStore';

/**
 * 토큰이 사라졌는데 화면이 로그인된 채로 남는 문제를 고정한다.
 *
 * <p>401 을 받은 api client 가 토큰을 지우지만 그 사실을 아무도 전하지 않던 시절이 있었다.
 * 조회마다 401 이 반복되고 사용자는 주소창에 /login 을 직접 쳐야 빠져나왔다.
 */
function Probe() {
  const auth = useAuth();
  return <span data-testid="status">{auth.status}</span>;
}

function renderAuth() {
  return render(
    <AuthProvider>
      <Probe />
    </AuthProvider>,
  );
}

const me = { email: 'admin@zzol.site' };

beforeEach(() => {
  localStorage.clear();
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => new Response(JSON.stringify(me), { status: 200 })),
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('AuthProvider', () => {
  it('저장된 토큰이 살아 있으면 로그인 상태로 들어간다', async () => {
    saveToken('live-token');

    renderAuth();

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));
  });

  it('토큰이 지워지면 로그인 상태에서 내려온다', async () => {
    // 401 을 받은 client 가 토큰을 지우는 상황이다. 이 전이가 없으면 화면은 로그인된 채로
    // 남고 조회만 계속 401 이 된다.
    saveToken('live-token');
    renderAuth();
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));

    act(() => clearToken());

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
  });

  it('다른 탭에서 로그아웃하면 이 탭도 따라 내려온다', async () => {
    saveToken('live-token');
    renderAuth();
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));

    // 다른 탭이 지운 것은 storage 이벤트로만 온다. 이 탭의 clearToken 은 불리지 않는다.
    act(() => {
      localStorage.removeItem('zzol-admin-token');
      window.dispatchEvent(
        new StorageEvent('storage', { key: 'zzol-admin-token', newValue: null }),
      );
    });

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
  });

  it('다른 탭에서 로그인하면 확인을 거쳐 돌아온다', async () => {
    renderAuth();
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));

    act(() => {
      localStorage.setItem('zzol-admin-token', 'fresh-token');
      window.dispatchEvent(
        new StorageEvent('storage', { key: 'zzol-admin-token', newValue: 'fresh-token' }),
      );
    });

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));
  });

  it('우리 키가 아닌 저장소 변화는 무시한다', async () => {
    saveToken('live-token');
    renderAuth();
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));

    act(() => {
      window.dispatchEvent(new StorageEvent('storage', { key: 'other-app-key', newValue: null }));
    });

    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
  });
});
