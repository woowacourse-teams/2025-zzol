import { Client, StompSubscription } from '@stomp/stompjs';
import { createContext, useContext } from 'react';
import type { WsRequestOf, WsSendDestination, WsSendPath } from '../generated/wsContract';

export type WebSocketContextType = {
  startSocket: (roomToken: string) => void;
  stopSocket: () => void;
  /** body 는 BE 요청 record 에서 생성한 타입이다. 요청 record 가 없는 destination 은 body 를 생략한다. */
  send: <D extends WsSendPath>(
    destination: WsSendDestination<D>,
    ...args: WsRequestOf<D> extends undefined
      ? [body?: undefined, onError?: (error: Error) => void]
      : [body: WsRequestOf<D>, onError?: (error: Error) => void]
  ) => void;
  subscribe: <T>(
    destination: string,
    onData: (data: T) => void,
    onError?: (error: Error) => void
  ) => StompSubscription | null;
  isConnected: boolean;
  client: Client | null;
  sessionId: string | null;
  isRecovering: boolean;
};

export const WebSocketContext = createContext<WebSocketContextType | null>(null);

export const useWebSocket = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket는 WebSocketProvider 안에서 사용해야 합니다.');
  }
  return context;
};
