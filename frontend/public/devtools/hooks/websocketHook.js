/* eslint-env browser */

import { addRequest } from '../utils/common/addRequest.js';
import { createWebSocketMessage } from '../utils/websocket/websocketMessageHandler.js';
import { getSafeWindow } from '../utils/common/getSafeWindow.js';
import { checkAlreadyHooked } from '../utils/common/checkAlreadyHooked.js';
import { defineHookedProperty } from '../utils/common/defineHookedProperty.js';

export const setupWebSocketHook = (win, collector, context = {}) => {
  win = getSafeWindow(win);
  if (!win) return null;

  if (checkAlreadyHooked(win, 'WebSocket')) {
    return null;
  }

  const OriginalWebSocket = win.WebSocket;

  // WebSocket 연결 추적을 위한 맵 (URL+컨텍스트를 키로 사용)
  const wsConnectionMap = new Map();

  const setupWebSocketTracking = (ws, url, collector, context) => {
    const connectionKey = `${context}:${url}`;
    const messages = [];

    ws.addEventListener('open', () => {
      try {
        const connectionStartTime = Date.now();
        const requestData = {
          type: 'websocket',
          context,
          url: String(url),
          status: 101,
          startedAt: connectionStartTime,
          messages: messages,
          connectionStatus: 'open',
        };
        // addRequest를 사용하여 공통 필드 보장하고 생성된 request 객체 반환
        const request = addRequest(collector, requestData);
        wsConnectionMap.set(connectionKey, request);
      } catch {
        /* noop */
      }
    });

    /**
     * 수신된 WebSocket 메시지를 처리하고 messages 배열에 추가합니다.
     */
    ws.addEventListener('message', (event) => {
      try {
        const message = createWebSocketMessage(event.data, 'received');
        // messages 배열에 직접 추가 (객체 참조가 공유되므로 collector에도 반영됨)
        messages.push(message);
      } catch {
        /* noop */
      }
    });

    ws.addEventListener('error', () => {
      try {
        const connection = wsConnectionMap.get(connectionKey);
        if (connection) {
          connection.connectionStatus = 'error';
        }
      } catch {
        /* noop */
      }
    });

    ws.addEventListener('close', () => {
      try {
        const connection = wsConnectionMap.get(connectionKey);
        if (connection) {
          connection.connectionStatus = 'closed';
          connection.durationMs = Date.now() - connection.startedAt;
        }
      } catch {
        /* noop */
      }
    });

    /**
     * WebSocket send 메서드를 훅킹하여 전송된 메시지를 추적합니다.
     */
    const originalSend = ws.send.bind(ws);
    ws.send = function (data) {
      try {
        const message = createWebSocketMessage(data, 'sent');
        // messages 배열에 직접 추가 (객체 참조가 공유되므로 collector에도 반영됨)
        messages.push(message);
      } catch {
        /* noop */
      }

      // 원본 send 메서드 호출
      return originalSend.call(this, data);
    };
  };

  const HookedWebSocket = new Proxy(OriginalWebSocket, {
    construct(target, args) {
      const [url] = args;
      const ws = new target(...args);

      try {
        setupWebSocketTracking(ws, url, collector, context);
      } catch {
        /* noop */
      }

      return ws;
    },
  });

  defineHookedProperty(win, 'WebSocket', HookedWebSocket);
};
