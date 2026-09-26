import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { refreshAccessToken } from '@/auth/refresh';
import { readToken, saveToken } from '@/auth/tokenStore';

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
