import { API_BASE_URL } from '@/lib/env';
import { readToken, saveToken } from '@/auth/tokenStore';

const LOCK_NAME = 'zzol-admin-refresh';

let inFlight: Promise<string | null> | null = null;

/**
 * refresh 쿠키로 새 액세스 토큰을 받는다.
 *
 * <p>서버는 refresh 토큰을 쓸 때마다 새 것으로 바꾸고, 이미 쓴 토큰이 다시 오면 탈취로 보고
 * 그 로그인을 끊는다. 그래서 <b>같은 쿠키로 두 번 부르면 안 된다.</b> 두 겹으로 막는다.
 *
 * <ul>
 *   <li><b>이 탭 안</b>: 동시에 401 을 받은 요청들이 진행 중인 재발급 하나를 함께 기다린다.
 *   <li><b>탭 사이</b>: Web Locks 로 한 번에 한 탭만 부른다. 락을 잡은 뒤 저장된 토큰이 방금
 *       실패한 토큰과 다르면 다른 탭이 이미 갱신한 것이라 그 값을 쓴다. 락은 응답을 받은 뒤에
 *       풀리므로 그때는 브라우저 쿠키도 이미 새 값이다.
 * </ul>
 *
 * <p>{@code navigator.locks} 가 없는 브라우저에서는 탭 안의 묶음만 동작한다.
 *
 * @param failedToken 401 을 받은 요청이 실어 보낸 토큰. 없었으면 null
 * @returns 새 토큰. 서버가 재발급을 거절했으면(401, 403) null
 * @throws 네트워크 오류나 5xx. 로그인이 끊긴 것이 아니므로 호출한 쪽이 토큰을 지우지 않게 한다
 */
export function refreshAccessToken(failedToken: string | null): Promise<string | null> {
  if (!inFlight) {
    inFlight = withRefreshLock(() => refreshUnlessDone(failedToken)).finally(() => {
      inFlight = null;
    });
  }
  return inFlight;
}

function withRefreshLock<T>(task: () => Promise<T>): Promise<T> {
  if (typeof navigator !== 'undefined' && navigator.locks) {
    return navigator.locks.request(LOCK_NAME, task);
  }
  return task();
}

async function refreshUnlessDone(failedToken: string | null): Promise<string | null> {
  // 저장된 토큰이 실패한 요청의 토큰과 다르면 그사이 누가 바꾼 것이다. 다른 탭이 갱신했으면
  // 그 토큰을 쓰고, 로그아웃으로 지워졌으면(null) 재발급하지 않는다. 로그아웃 직전에 나간
  // 요청의 401 로 재발급하면 로그아웃한 화면이 다시 로그인된다.
  const current = readToken();
  if (current !== failedToken) {
    return current;
  }

  const response = await fetch(`${API_BASE_URL}/admin/api/auth/refresh`, {
    method: 'POST',
    // 운영에서 admin 과 api 는 오리진이 달라서, 이것이 없으면 쿠키가 실리지 않는다.
    credentials: 'include',
  });
  if (response.status === 401 || response.status === 403) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`토큰 재발급 실패 (${response.status})`);
  }

  const { accessToken } = (await response.json()) as { accessToken: string };
  // 응답을 기다리는 동안 로그아웃했으면 새 토큰을 버린다. 서버의 refresh 는 로그아웃이 지우지만
  // 이 access 토큰은 1시간 동안 유효해서, 저장하면 로그아웃한 화면이 다시 로그인된다.
  if (readToken() !== current) {
    return null;
  }
  saveToken(accessToken);
  return accessToken;
}
