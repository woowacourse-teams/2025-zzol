/* eslint-env browser */

import { NetworkCollector } from '../../core/networkCollector.js';
import { MARKERS } from './markers.js';

const MAX_REQUESTS = 1000;

/**
 * NetworkCollector를 초기화하고 윈도우 객체에 저장합니다.
 * 이미 존재하는 경우 기존 collector를 반환합니다.
 */
export const initializeCollector = (win) => {
  if (!win[MARKERS.COLLECTOR]) {
    try {
      win[MARKERS.COLLECTOR] = new NetworkCollector(MAX_REQUESTS);
    } catch {
      /* noop */
    }
  }
  return win[MARKERS.COLLECTOR];
};
