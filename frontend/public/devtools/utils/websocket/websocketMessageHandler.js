/* eslint-env browser */

import { parseStompMessage } from './stompParser.js';

/**
 * WebSocket 데이터를 문자열로 변환합니다.
 * Blob, ArrayBuffer 등의 바이너리 데이터는 설명 문자열로 변환합니다.
 *
 * @param {string|Blob|ArrayBuffer} data - 변환할 데이터
 * @returns {string} 문자열로 변환된 데이터
 */
const convertDataToString = (data) => {
  if (typeof data === 'string') {
    return data;
  }

  // eslint-disable-next-line no-undef
  if (typeof Blob !== 'undefined' && data instanceof Blob) {
    return '[Blob data]';
  }

  if (typeof ArrayBuffer !== 'undefined' && data instanceof ArrayBuffer) {
    return '[ArrayBuffer data]';
  }

  return String(data);
};

/**
 * WebSocket 메시지 객체를 생성합니다.
 * STOMP 메시지인 경우 파싱하여 구조화된 형태로 저장합니다.
 *
 * @param {string|Blob|ArrayBuffer} rawData - 원본 메시지 데이터
 * @param {'sent'|'received'} type - 메시지 타입 (송신/수신)
 * @returns {{type: 'sent'|'received', data: string, timestamp: number, isStompMessage: boolean, stompHeaders?: Record<string, string>, stompBody?: string}} 생성된 메시지 객체
 */
export const createWebSocketMessage = (rawData, type) => {
  const sendData = convertDataToString(rawData);

  // STOMP 메시지 파싱 시도
  const stompParsed = parseStompMessage(sendData);

  if (stompParsed && typeof stompParsed === 'object' && stompParsed.headers) {
    return {
      type,
      data: sendData,
      timestamp: Date.now(),
      isStompMessage: true,
      stompHeaders: stompParsed.headers,
      stompBody: stompParsed.body,
    };
  }

  return {
    type,
    data: sendData,
    timestamp: Date.now(),
    isStompMessage: false,
    stompHeaders: undefined,
    stompBody: undefined,
  };
};
