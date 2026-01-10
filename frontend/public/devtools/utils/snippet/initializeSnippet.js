/* eslint-env browser */

import { MARKERS } from './markers.js';

/**
 * 스니펫 초기화 마커를 설정하고 활성화 로그를 출력합니다.
 */
export const initializeSnippet = (win) => {
  win[MARKERS.SNIPPET] = true;
  win.console && win.console.log('[DEV SNIPPET] active');
};

