import { User, Tokens } from '../types';

const API_URL = process.env.API_URL;

// auth 전용 fetch — apiRequest 인터셉터를 우회해 무한 401 루프 방지
const authFetch = async <T>(
  endpoint: string,
  options: { method?: string; body?: string } = {}
): Promise<T> => {
  const response = await fetch(`${API_URL}${endpoint}`, {
    headers: { 'Content-Type': 'application/json' },
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
  me: (): Promise<User> => authFetch('/users/me'),

  refresh: (refreshToken: string): Promise<Tokens> =>
    authFetch('/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),

  logout: (): Promise<void> => authFetch('/auth/logout', { method: 'POST' }),

  updateNickname: (nickname: string): Promise<User> =>
    authFetch('/users/me', {
      method: 'PATCH',
      body: JSON.stringify({ nickname }),
    }),
};
