export const WEBSOCKET_CONFIG = {
  TOPIC_PREFIX: '/topic',
  APP_PREFIX: '/app',
} as const;

export type WebSocketSuccess<T> = {
  success: true;
  data: T;
  errorMessage: null;
  id: string | null;
};

export type WebSocketError = {
  success: false;
  data: null;
  errorMessage: string;
};

export type WebSocketErrorOptions = {
  type?: WebSocketErrorType;
  extra?: Record<string, unknown>;
};

export type WebSocketMessage<T> = WebSocketSuccess<T> | WebSocketError;

export type WebSocketErrorType = 'stomp' | 'connection' | 'subscription' | 'send' | 'parsing';
