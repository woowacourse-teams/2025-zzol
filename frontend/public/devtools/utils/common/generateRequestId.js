/* eslint-env browser */

/**
 * 고유 ID 생성 유틸리티
 */
export const generateRequestId = () => {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
};

