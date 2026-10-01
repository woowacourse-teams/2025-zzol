import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

/**
 * 저장이 막힌 브라우저를 흉내 낸다. 모듈이 "저장소가 망가졌다"는 사실을 기억하므로
 * 테스트마다 새로 불러온다.
 */
async function loadStore() {
  vi.resetModules();
  return import('@/auth/tokenStore');
}

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('tokenStore', () => {
  it('저장소에 쓰지 못해도 이 탭 안에서는 토큰을 읽는다', async () => {
    // 못 읽으면 요청마다 401 을 받고 refresh 를 회전해 요청이 두 배가 된다.
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('저장 공간이 없습니다', 'QuotaExceededError');
    });
    const { readToken, saveToken } = await loadStore();

    saveToken('token-in-memory');

    expect(readToken()).toBe('token-in-memory');
  });

  it('저장소에 쓰지 못한 탭에서도 로그아웃하면 토큰이 사라진다', async () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('저장 공간이 없습니다', 'QuotaExceededError');
    });
    const { clearToken, readToken, saveToken } = await loadStore();
    saveToken('token-in-memory');

    clearToken();

    expect(readToken()).toBeNull();
  });

  it('저장소가 정상이면 다른 탭에서 지운 것을 따라간다', async () => {
    // 메모리 사본이 저장소보다 앞서면 다른 탭의 로그아웃을 이 탭이 못 따라간다.
    const { readToken, saveToken } = await loadStore();
    saveToken('shared-token');

    localStorage.removeItem('zzol-admin-token');

    expect(readToken()).toBeNull();
  });
});
