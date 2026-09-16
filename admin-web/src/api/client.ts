import { API_BASE_URL } from '@/lib/env';
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

/**
 * 모든 API 호출이 지나는 한 곳.
 *
 * <p>401 을 받으면 토큰을 지운다. 서버가 이미 거절한 토큰을 계속 들고 있으면 화면마다
 * 401 이 반복되고, 사용자는 새로고침해도 안 고쳐지는 상태에 갇힌다.
 *
 * <p>401 자동 재발급은 하지 않는다. 관리자 토큰에는 리프레시가 없고, 재발급하려면
 * 구글 팝업이 다시 떠야 하는데 그것을 백그라운드에서 몰래 하면 팝업 차단에 걸린다.
 * 로그인 화면으로 돌려보내 사람이 누르게 한다.
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

  const token = readToken();
  const response = await fetch(url, {
    method,
    signal,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401) {
    clearToken();
  }

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
