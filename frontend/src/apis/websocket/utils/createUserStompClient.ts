import { Client, ReconnectionTimeMode } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getWebSocketUrl } from './getWebSocketUrl';

export const createUserStompClient = (accessToken: string) => {
  const wsUrl = getWebSocketUrl();

  return new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    debug: (msg) => console.log('[USER-STOMP]', msg),
    reconnectDelay: 1000,
    reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
    maxReconnectDelay: 30000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    connectHeaders: { Authorization: `Bearer ${accessToken}` },
  });
};
