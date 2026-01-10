/* eslint-env browser */

/**
 * Request 정보 추출 유틸리티
 */
export const extractRequestInfo = (input, init) => {
  try {
    if (typeof input === 'string') return { method: (init && init.method) || 'GET', url: input };
    if (input && input.url) return { method: input.method || 'GET', url: input.url };
  } catch {
    /* noop */
  }
  return { method: 'GET', url: String(input) };
};

