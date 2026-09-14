const KEY = 'zzol-admin-token';

/**
 * 관리자 토큰 보관소.
 *
 * <p>`localStorage` 를 쓴다. 메모리에만 두면 새로고침마다 구글 팝업이 다시 떠야 하는데,
 * 백오피스는 표를 보다 새로고침하는 일이 잦아 그 비용이 크다.
 *
 * <p>XSS 가 나면 토큰이 털린다는 반론이 있지만, 이 앱은 사용자 입력을 innerHTML 로 그리는
 * 곳이 없고 서드파티 스크립트도 구글 하나뿐이라 실질 노출면이 좁다. 그리고 토큰 수명이
 * 1시간이고 리프레시가 없어 털려도 창이 짧다.
 *
 * <p>접근이 막힌 브라우저(시크릿 모드 설정, 저장소 차단)에서도 앱이 죽지 않아야 하므로
 * 모든 접근을 try 로 감싼다. 실패하면 "로그인 안 된 상태"로 흘러가면 된다.
 *
 * <p>토큰이 바뀌면 {@link subscribeToken} 으로 알린다. 저장소를 <b>읽는 쪽이 여럿</b>이라
 * 그렇다. 화면은 AuthProvider 의 상태를 보고 그리는데 토큰을 지우는 것은 401 을 받은
 * api client 라, 알림이 없으면 둘이 어긋난다. 실제로 그 상태에서 화면은 로그인된 것처럼
 * 남고 조회만 계속 401 이 됐다.
 */

type Listener = (token: string | null) => void;

const listeners = new Set<Listener>();

function notify(): void {
  const token = readToken();
  for (const listener of listeners) {
    listener(token);
  }
}
export function readToken(): string | null {
  try {
    return localStorage.getItem(KEY);
  } catch {
    return null;
  }
}

export function saveToken(token: string): void {
  try {
    localStorage.setItem(KEY, token);
  } catch {
    // 저장이 막힌 브라우저에서는 이 탭 안에서만 로그인 상태가 유지된다.
  }
  notify();
}

export function clearToken(): void {
  try {
    localStorage.removeItem(KEY);
  } catch {
    // 지우지 못해도 서버가 거절하므로 실질 위험은 없다.
  }
  notify();
}

/**
 * 토큰이 바뀔 때 알림을 받는다. 해지 함수를 돌려준다.
 *
 * <p>두 경로를 모두 듣는다.
 *
 * <ul>
 *   <li><b>이 탭</b>: {@link saveToken}, {@link clearToken} 이 직접 부른다. storage 이벤트는
 *       바꾼 탭에는 오지 않으므로, 401 로 토큰이 지워진 것을 그 탭이 알 방법이 이것뿐이다.
 *   <li><b>다른 탭</b>: storage 이벤트. 한쪽에서 로그아웃하면 열어 둔 나머지 탭도 같이
 *       내려가야 한다. 반대로 한쪽에서 다시 로그인하면 나머지 탭이 새로고침 없이 돌아온다.
 * </ul>
 */
export function subscribeToken(listener: Listener): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    // key 가 null 이면 localStorage.clear() 다. 우리 키도 함께 날아갔다고 봐야 한다.
    if (event.key === null || event.key === KEY) {
      notify();
    }
  });
}
