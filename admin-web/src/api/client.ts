import { API_BASE_URL } from '@/lib/env';
import { refreshAccessToken } from '@/auth/refresh';
import { clearToken, readToken } from '@/auth/tokenStore';

const PREFIX = '/admin/api';

/** 서버가 ProblemDetail 로 내려주는 오류. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    /** 서버가 준 ErrorCode. NOT_ADMIN, ADMIN_TOKEN_EXPIRED 같은 값. */
    readonly code: string | undefined,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }

  /** 토큰이 없거나 만료됐다. 다시 로그인하면 풀린다. */
  get isUnauthorized() {
    return this.status === 401;
  }

  /** 허용목록에 없다. 다시 로그인해도 소용없다. */
  get isForbidden() {
    return this.status === 403;
  }
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  /** 쿼리 파라미터. undefined 와 빈 문자열은 빠진다. */
  params?: Record<string, string | number | boolean | undefined | null>;
  signal?: AbortSignal;
};

/** refresh 쿠키를 주고받는 경로. 여기서 받은 401 은 재발급으로 풀 수 없다. */
const COOKIE_AUTH_PATHS = ['/auth/login', '/auth/refresh', '/auth/logout'];

type AuthorizedInit = Omit<RequestInit, 'headers'> & { headers?: Record<string, string> };

/**
 * 관리자 토큰을 붙여 보내고, 401 이면 refresh 쿠키로 재발급받아 한 번 더 보낸다.
 *
 * <p>재발급까지 거절되면 토큰을 지운다. 서버가 이미 거절한 토큰을 계속 들고 있으면 화면마다
 * 401 이 반복되고, 사용자는 새로고침해도 안 고쳐지는 상태에 갇힌다. 지우면 AuthProvider 가
 * 따라 내려가 로그인 화면으로 보낸다.
 *
 * <p>재발급이 네트워크 오류로 실패하면 토큰을 지우지 않는다. 로그인이 끊긴 것이 아니다.
 *
 * <p>본문을 두 번 보내므로 스트림 본문은 넘기지 않는다. 문자열만 쓴다.
 */
export async function authorizedFetch(input: URL | string, init: AuthorizedInit = {}): Promise<Response> {
  const send = (token: string | null) =>
    fetch(input, {
      ...init,
      headers: { ...init.headers, ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    });

  const token = readToken();
  const response = await send(token);
  if (response.status !== 401) {
    return response;
  }

  let refreshed: string | null;
  try {
    refreshed = await refreshAccessToken(token);
  } catch {
    return response;
  }
  if (!refreshed) {
    clearToken();
    return response;
  }

  const retried = await send(refreshed);
  if (retried.status === 401) {
    clearToken();
  }
  return retried;
}

/**
 * 모든 API 호출이 지나는 한 곳. 인증과 재발급은 {@link authorizedFetch} 가 맡는다.
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, params, signal } = options;

  const url = new URL(`${API_BASE_URL}${PREFIX}${path}`, window.location.origin);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, String(value));
      }
    }
  }

  const init: AuthorizedInit = {
    method,
    signal,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
  };
  const isCookieAuth = COOKIE_AUTH_PATHS.includes(path);
  const response = isCookieAuth
    ? // 로그인 응답의 쿠키를 저장하고 logout 에 쿠키를 싣는다. 오리진이 달라 명시해야 한다.
      await fetch(url, { ...init, credentials: 'include' })
    : await authorizedFetch(url, init);

  if (!response.ok) {
    throw await toApiError(response);
  }

  // 204 와 빈 본문은 JSON 파싱을 시도하면 터진다.
  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    // 스프링 RestExceptionHandler 가 ProblemDetail 로 내려준다.
    // 코드 필드 이름은 `code` 가 아니라 `errorCode` 다. 실제 응답을 찍어 확인했다.
    // {"title":"Forbidden","status":403,"detail":"...","errorCode":"NOT_ADMIN",...}
    const problem = (await response.json()) as {
      detail?: string;
      title?: string;
      errorCode?: string;
    };
    return new ApiError(
      response.status,
      problem.errorCode,
      problem.detail ?? problem.title ?? `요청이 실패했습니다 (${response.status})`,
    );
  } catch {
    return new ApiError(response.status, undefined, `요청이 실패했습니다 (${response.status})`);
  }
}

export const api = {
  get: <T>(path: string, params?: RequestOptions['params']) =>
    request<T>(path, { params }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};
