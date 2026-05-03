import { apiRequest } from '@/apis/rest/apiRequest';
import { User, Tokens, OAuthCallbackResponse } from '../types';

const API_URL = process.env.API_URL;

// auth 전용 fetch — apiRequest 인터셉터를 우회해 무한 401 루프 방지
const authFetch = async <T>(
  endpoint: string,
  options: { method?: string; body?: string; token?: string } = {}
): Promise<T> => {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (options.token) headers['Authorization'] = `Bearer ${options.token}`;

  const response = await fetch(`${API_URL}${endpoint}`, {
    headers,
    credentials: 'include',
    method: options.method,
    body: options.body,
  });

  if (!response.ok) {
    throw new Error(`auth API error: ${response.status}`);
  }

  if (response.status === 204) return {} as T;

  const text = await response.text();
  return text ? JSON.parse(text) : ({} as T);
};

export const authApi = {
  me: (): Promise<User> => apiRequest('/users/me'),

  // code를 accessToken으로 교환 — refreshToken은 HttpOnly 쿠키로 자동 설정됨
  token: (code: string): Promise<OAuthCallbackResponse> =>
    authFetch('/auth/token', {
      method: 'POST',
      body: JSON.stringify({ code }),
    }),

  agreeTerms: (tempToken: string): Promise<Tokens> =>
    authFetch('/users/me/terms', { method: 'POST', token: tempToken }),

  // refreshToken은 쿠키로 자동 전송 — body 불필요
  refresh: (): Promise<Tokens> => authFetch('/auth/refresh', { method: 'POST' }),

  logout: (): Promise<void> => authFetch('/auth/logout', { method: 'POST' }),

  updateNickname: (nickname: string): Promise<User> =>
    apiRequest('/users/me', { method: 'PATCH', body: { nickname } }),
};
