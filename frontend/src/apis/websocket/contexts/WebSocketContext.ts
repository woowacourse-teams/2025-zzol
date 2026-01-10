import { Client, StompSubscription } from '@stomp/stompjs';
import { createContext, useContext } from 'react';

export type WebSocketContextType = {
  startSocket: (joinCode: string, myName: string) => void;
  stopSocket: () => void;
  send: <T>(destination: string, body?: T, onError?: (error: Error) => void) => void;
  subscribe: <T>(
    destination: string,
    onData: (data: T) => void,
    onError?: (error: Error) => void
  ) => StompSubscription | null;
  isConnected: boolean;
  client: Client | null;
  sessionId: string | null;
};

export const WebSocketContext = createContext<WebSocketContextType | null>(null);

export const useWebSocket = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket는 WebSocketProvider 안에서 사용해야 합니다.');
  }
  return context;
};
