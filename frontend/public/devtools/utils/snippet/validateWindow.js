/* eslint-env browser */

import { MARKERS } from './markers.js';

/**
 * 윈도우 객체의 유효성을 검증합니다.
 * 브라우저 환경인지, 이미 초기화되었는지 확인합니다.
 */
export const validateWindow = (win) => {
  if (!win) {
    // non-browser guard
    throw new Error('dev-snippet.js requires browser environment');
  }
  if (win[MARKERS.SNIPPET]) {
    // Already initialized
    throw new Error('dev-snippet.js already initialized');
  }
};
