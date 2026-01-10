/* eslint-env browser */

import { generateRequestId } from './generateRequestId.js';

/**
 * 네트워크 요청을 공통 포맷으로 컬렉터에 추가합니다.
 * 모든 네트워크 요청 타입(fetch, websocket, stomp 등)에서 공통 필드를 보장합니다.
 */
export const addRequest = (collector, data) => {
  const now = Date.now();

  const base = {
    id: generateRequestId(),
    timestamp: now,
    durationMs: 0,
    type: 'unknown',
    status: null,
    context: {},
    url: '',
  };

  // durationMs 계산: data.durationMs가 있으면 사용, 없으면 startedAt 기반 계산
  let durationMs = data.durationMs;
  if (durationMs === undefined && data.startedAt) {
    durationMs = now - data.startedAt;
  }

  const request = {
    ...base,
    ...data,
    durationMs: durationMs ?? 0,
  };

  collector.add(request);
  return request;
};

