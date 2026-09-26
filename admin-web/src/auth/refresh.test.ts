import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { refreshAccessToken } from '@/auth/refresh';
import { clearToken, readToken, saveToken } from '@/auth/tokenStore';

function refreshCalls(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/auth/refresh')).length;
}

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('refreshAccessToken', () => {
  it('동시에 여러 번 불러도 재발급은 한 번만 나간다', async () => {
    // 서버는 이미 쓴 refresh 토큰이 다시 오면 탈취로 보고 로그인을 끊는다.
    // 화면 하나가 조회 여러 개를 동시에 띄우므로 401 도 동시에 여러 개 온다.
    const fetchMock = vi.fn(
      async () => new Response(JSON.stringify({ accessToken: 'new' }), { status: 200 }),
    );
    vi.stubGlobal('fetch', fetchMock);
    saveToken('expired');

    const results = await Promise.all([
      refreshAccessToken('expired'),
      refreshAccessToken('expired'),
      refreshAccessToken('expired'),
    ]);

    expect(results).toEqual(['new', 'new', 'new']);
    expect(refreshCalls(fetchMock)).toBe(1);
    expect(readToken()).toBe('new');
  });

  it('다른 탭이 이미 갱신했으면 서버를 부르지 않고 그 토큰을 쓴다', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    saveToken('refreshed-by-other-tab');

    await expect(refreshAccessToken('expired')).resolves.toBe('refreshed-by-other-tab');
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('재발급 응답이 오기 전에 로그아웃하면 새 토큰을 저장하지 않는다', async () => {
    // 저장하면 로그아웃한 화면이 새 토큰으로 다시 로그인 상태가 된다. 서버의 refresh 는
    // 로그아웃이 지우지만 이미 받은 access 토큰은 1시간 동안 유효하다.
    saveToken('expired');
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        clearToken();
        return new Response(JSON.stringify({ accessToken: 'new' }), { status: 200 });
      }),
    );

    await expect(refreshAccessToken('expired')).resolves.toBeNull();
    expect(readToken()).toBeNull();
  });

  it('로그아웃한 뒤 늦게 도착한 401 로는 재발급하지 않는다', async () => {
    // 로그아웃 직전에 나간 조회가 옛 토큰으로 401 을 받은 경우다. 서버가 아직 로그아웃을
    // 처리하지 않았으면 쿠키로 재발급이 성공해 다시 로그인된다.
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    await expect(refreshAccessToken('token-before-logout')).resolves.toBeNull();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('서버가 거절하면 null 이다', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 401 })));

    await expect(refreshAccessToken(null)).resolves.toBeNull();
  });

  it('서버 오류는 던진다', async () => {
    // 로그인이 끊긴 것과 구분해야 호출한 쪽이 토큰을 지우지 않는다.
    // 본문을 붙인다. 비어 있으면 상태 검사가 없어도 JSON 파싱이 대신 터져 이 테스트가 아무것도 못 잰다.
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response(JSON.stringify({ status: 502 }), { status: 502 })),
    );

    await expect(refreshAccessToken(null)).rejects.toThrow();
    expect(readToken()).toBeNull();
  });

  it('쿠키가 실리도록 credentials 를 켠다', async () => {
    const fetchMock = vi.fn(async () => new Response(null, { status: 401 }));
    vi.stubGlobal('fetch', fetchMock);

    await refreshAccessToken(null);

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/admin/api/auth/refresh'),
      expect.objectContaining({ method: 'POST', credentials: 'include' }),
    );
  });
});
