import { ApiError, ErrorDisplayMode, NetworkError } from './error';
import { reportApiError } from '@/apis/utils/reportSentryError';

const API_URL = process.env.API_URL;

export type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export type ApiRequestOptions<TData> = {
  method?: Method;
  headers?: Record<string, string>;
  body?: TData;
  retry?: {
    count: number;
    delay: number;
  };
  errorDisplayMode?: ErrorDisplayMode;
  bypassAuth?: boolean;
};

type AuthInterceptor = {
  getAccessToken: () => string | null;
  refresh: () => Promise<string | null>;
  onExpired: () => void;
};

let authInterceptor: AuthInterceptor | null = null;
let refreshingPromise: Promise<string | null> | null = null;

export const setAuthInterceptor = (interceptor: AuthInterceptor) => {
  authInterceptor = interceptor;
};

export type ApiConfig = {
  method: Method;
  headers: Record<string, string>;
  body: string | null;
};

export const apiRequest = async <T, TData>(
  url: string,
  options: ApiRequestOptions<TData> = {}
): Promise<T> => {
  const {
    method = 'GET',
    headers = {},
    body = null,
    retry = { count: 0, delay: 1000 },
    errorDisplayMode = options.errorDisplayMode || (method === 'GET' ? 'fallback' : 'toast'),
    bypassAuth = false,
  } = options;

  let requestUrl = API_URL + url;

  const buildHeaders = (): Record<string, string> => {
    const base: Record<string, string> = {
      'Content-Type': 'application/json',
      ...headers,
    };
    if (!bypassAuth && authInterceptor) {
      const token = authInterceptor.getAccessToken();
      if (token) base['Authorization'] = `Bearer ${token}`;
    }
    return base;
  };

  const parsedBody = body ? JSON.stringify(body) : null;

  const makeRequest = async (retryCount = 0): Promise<T> => {
    try {
      const fetchOptions = {
        method: method,
        headers: buildHeaders(),
        body: method !== 'GET' && parsedBody ? parsedBody : null,
        credentials: 'include' as const,
      };

      const response = await fetch(requestUrl, fetchOptions);

      if (response.status === 401 && !bypassAuth && authInterceptor) {
        try {
          if (!refreshingPromise) {
            refreshingPromise = authInterceptor.refresh().finally(() => {
              refreshingPromise = null;
            });
          }
          const newToken = await refreshingPromise;

          if (!newToken) {
            authInterceptor.onExpired();
            throw new ApiError({
              status: 401,
              message: '세션이 만료되었습니다. 다시 로그인해 주세요.',
              displayMode: 'toast',
            });
          }

          // 새 토큰으로 원 요청 1회 재시도
          const retryResponse = await fetch(requestUrl, {
            ...fetchOptions,
            headers: { ...buildHeaders(), Authorization: `Bearer ${newToken}` },
          });

          if (!retryResponse.ok) {
            authInterceptor.onExpired();
            throw new ApiError({
              status: retryResponse.status,
              message: '세션이 만료되었습니다. 다시 로그인해 주세요.',
              displayMode: 'toast',
            });
          }

          if (retryResponse.status === 204) return {} as T;
          const retryText = await retryResponse.text();
          return retryText ? JSON.parse(retryText) : ({} as T);
        } catch (refreshError) {
          if (refreshError instanceof ApiError) throw refreshError;
          authInterceptor.onExpired();
          throw new ApiError({
            status: 401,
            message: '세션이 만료되었습니다. 다시 로그인해 주세요.',
            displayMode: 'toast',
          });
        }
      }

      if (!response.ok) {
        let errorData = null;
        let errorMessage = `HTTP ${response.status} Error`;

        try {
          const contentType = response.headers.get('content-type');

          if (
            contentType &&
            (contentType.includes('application/json') ||
              contentType.includes('application/problem+json'))
          ) {
            errorData = await response.json();
            errorMessage = errorData.detail;
          } else {
            const textError = await response.text();
            errorMessage = textError || errorMessage;
          }
        } catch (parseError) {
          console.warn('응답 메시지 파싱 실패', parseError);
        }

        const apiError = new ApiError({
          status: response.status,
          message: errorMessage,
          data: errorData,
          displayMode: errorDisplayMode,
        });
        reportApiError(apiError);
        throw apiError;
      }

      if (response.status === 204) {
        return {} as T;
      }

      const text = await response.text();
      if (!text) {
        return {} as T;
      }

      return JSON.parse(text);
    } catch (error) {
      if (retryCount < retry.count) {
        console.warn(`재시도 중 (${retryCount + 1}/${retry.count})`);
        await new Promise((resolve) => setTimeout(resolve, retry.delay));

        return makeRequest(retryCount + 1);
      }

      if (error instanceof ApiError) {
        throw error;
      }

      if (error instanceof TypeError) {
        if (error.message.includes('fetch') || error.message.includes('Failed to fetch')) {
          const networkError = new NetworkError({
            message: error.message,
            displayMode: errorDisplayMode,
          });
          reportApiError(networkError);
          throw networkError;
        }
      }

      throw error;
    }
  };

  return makeRequest();
};
