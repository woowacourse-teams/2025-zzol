/* eslint-env browser */

/**
 * window 객체를 안전하게 가져옵니다.
 * 다양한 환경에서 window 접근을 시도합니다.
 *
 * @param {Window|undefined} win - 전달받은 window 객체
 * @returns {Window|undefined} 안전하게 가져온 window 객체 또는 undefined
 */
export const getSafeWindow = (win) => {
  if (win) return win;
  if (typeof globalThis !== 'undefined' && globalThis.window) return globalThis.window;
  return undefined;
};
