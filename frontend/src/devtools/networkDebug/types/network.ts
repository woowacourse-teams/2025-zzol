export type WebSocketMessage = {
  type: 'received' | 'sent';
  data: string;
  timestamp: number;
  // STOMP MESSAGE 관련
  isStompMessage?: boolean;
  stompHeaders?: Record<string, string>;
  stompBody?: string;
};

export type NetworkRequest = {
  id: string;
  type: 'fetch' | 'websocket';
  context: string;
  url: string;
  timestamp: number;
  method?: string;
  status?: number | string;
  responseBody?: string | null;
  durationMs?: number;
  data?: string;
  errorMessage?: string;
  // WebSocket 관련
  messages?: WebSocketMessage[];
  connectionStatus?: 'connecting' | 'open' | 'error' | 'closed';
};

export type NetworkCollector = {
  getRequests: () => NetworkRequest[];
  clear: () => void;
  subscribe: (listener: (request: NetworkRequest) => void) => () => void;
};
