export const WEBSOCKET_CONFIG = {
  TOPIC_PREFIX: '/topic',
  APP_PREFIX: '/app',
} as const;

// STOMP broker 가 자체 routing 하는 destination prefix. 이 prefix 로 시작하면 /topic 을 덧붙이지 않는다.
export const BROKER_PREFIXES = ['/user/', '/queue/'] as const;

export const isBrokerDestination = (destination: string): boolean =>
  BROKER_PREFIXES.some((prefix) => destination.startsWith(prefix));

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

// handleSubscriptionError 가 WebSocketError.extra 에 싣는 구조 — 소비자가 errorMessage 원문을 narrowing 한다.
export type SubscriptionErrorExtra = {
  url: string;
  messageBody?: string;
  errorMessage: string;
};

export type WebSocketErrorType = 'stomp' | 'connection' | 'subscription' | 'send' | 'parsing';
