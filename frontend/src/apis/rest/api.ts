import { apiRequest, ApiRequestOptions } from './apiRequest';

export const api = {
  get: <T>(url: string, options: Omit<ApiRequestOptions<T>, 'method' | 'data'> = {}) =>
    apiRequest<T, undefined>(url, { ...options, method: 'GET', body: undefined }),

  post: <T, TData>(
    url: string,
    data?: TData,
    options: Omit<ApiRequestOptions<TData>, 'method' | 'data'> = {}
  ) => apiRequest<T, TData>(url, { ...options, method: 'POST', body: data }),

  put: <T, TData>(
    url: string,
    data?: TData,
    options: Omit<ApiRequestOptions<TData>, 'method' | 'data'> = {}
  ) => apiRequest<T, TData>(url, { ...options, method: 'PUT', body: data }),

  patch: <T, TData>(
    url: string,
    data?: TData,
    options: Omit<ApiRequestOptions<TData>, 'method' | 'data'> = {}
  ) => apiRequest<T, TData>(url, { ...options, method: 'PATCH', body: data }),

  delete: <T>(url: string, options: Omit<ApiRequestOptions<T>, 'method' | 'data'> = {}) =>
    apiRequest<T, undefined>(url, { ...options, method: 'DELETE', body: undefined }),
};
