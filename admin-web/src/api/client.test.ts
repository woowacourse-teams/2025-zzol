import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from '@/api/client';
import { readToken, saveToken } from '@/auth/tokenStore';

type Route = (init?: RequestInit) => Response | Promise<Response>;

/** 주소별 응답. 등록하지 않은 주소는 이 테스트가 모르는 호출이라 터뜨린다. */
function stubServer(routes: Record<string, Route>) {
  const fetchMock = vi.fn(async (input: URL | string, init?: RequestInit) => {
    const path = new URL(String(input), 'http://localhost').pathname.replace('/admin/api', '');
    const route = routes[path];
    if (!route) throw new Error(`예상하지 못한 호출: ${path}`);
    return route(init);
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function bearerOf(init?: RequestInit) {
  return ((init?.headers ?? {}) as Record<string, string>).Authorization;
}

const ok = (body: unknown) => new Response(JSON.stringify(body), { status: 200 });
const unauthorized = () => new Response(null, { status: 401 });

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('request 의 401 처리', () => {
  it('재발급받은 토큰으로 한 번 더 보낸다', async () => {
    saveToken('expired');
    stubServer({
      '/rooms': (init) => (bearerOf(init) === 'Bearer fresh' ? ok({ rooms: 1 }) : unauthorized()),
      '/auth/refresh': () => ok({ accessToken: 'fresh' }),
    });

    await expect(api.get('/rooms')).resolves.toEqual({ rooms: 1 });
    expect(readToken()).toBe('fresh');
  });

  it('재발급도 거절되면 토큰을 지우고 401 을 던진다', async () => {
    saveToken('expired');
    stubServer({ '/rooms': unauthorized, '/auth/refresh': unauthorized });

    const error = await api.get('/rooms').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).isUnauthorized).toBe(true);
    expect(readToken()).toBeNull();
  });

  it('재발급이 네트워크 오류면 토큰을 지우지 않는다', async () => {
    // 로그인이 끊긴 것이 아니다. 지우면 잠깐의 오류로 로그인 화면에 튕긴다.
    saveToken('expired');
    stubServer({
      '/rooms': unauthorized,
      '/auth/refresh': () => {
        throw new TypeError('Failed to fetch');
      },
    });

    await expect(api.get('/rooms')).rejects.toBeInstanceOf(ApiError);
    expect(readToken()).toBe('expired');
  });

  it('로그인 요청의 401 은 재발급하지 않는다', async () => {
    const fetchMock = stubServer({ '/auth/login': unauthorized });

    await expect(api.post('/auth/login', { idToken: 'x' })).rejects.toBeInstanceOf(ApiError);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ credentials: 'include' }),
    );
  });
});
