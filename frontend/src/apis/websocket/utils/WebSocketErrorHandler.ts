import { reportWebSocketError } from '@/apis/utils/reportSentryError';
import { Client, IFrame, StompSocketState } from '@stomp/stompjs';
import { SubscriptionErrorExtra, WebSocketErrorOptions } from '../constants/constants';

export class WebSocketError extends Error {
  constructor(
    public message: string,
    public type: string,
    public extra?: Record<string, unknown>
  ) {
    super(message);
    this.name = 'WebSocketError';
    this.type = type;
    this.extra = extra;
  }
}

type SubscriptionErrorParams = {
  url: string;
  errorMessage: string;
  messageBody?: string;
  onError?: (error: WebSocketError) => void;
};

type ConnectionRequiredErrorParams = {
  type: 'subscription' | 'send';
  url: string;
  isConnected: boolean;
  hasClient: boolean;
  onError?: (error: WebSocketError) => void;
};

type ParsingErrorParams = {
  url: string;
  originalError: unknown;
  messageBody: string;
  onError?: (error: WebSocketError) => void;
};

type SendErrorParams = {
  url: string;
  originalError: unknown;
  body: unknown;
  onError?: (error: WebSocketError) => void;
};

class WebSocketErrorHandler {
  static handleError(
    message: string,
    options?: WebSocketErrorOptions,
    onError?: (error: WebSocketError) => void
  ): WebSocketError {
    console.error(message);
    reportWebSocketError(message, options);

    const error = new WebSocketError(message, options?.type || 'unknown', options?.extra);
    onError?.(error);
    return error;
  }

  static handleStompError(frame: IFrame): WebSocketError {
    const errorDetails = {
      command: frame.command,
      message: frame.headers['message'] || '알 수 없는 STOMP 오류',
      body: frame.body,
    };

    const errorMessage = `STOMP 오류 [${errorDetails.command}]: ${errorDetails.message}`;

    return this.handleError(errorMessage, {
      type: 'stomp',
      extra: { errorDetails },
    });
  }

  static handleWebSocketError(event: Event, stompClient: Client): WebSocketError {
    const readyState = stompClient.webSocket?.readyState;
    const readyStateText = readyState !== undefined ? StompSocketState[readyState] : 'UNKNOWN';

    const errorMessage = `WebSocket 연결 오류: ${event.type} (상태: ${readyStateText})`;

    return this.handleError(errorMessage, {
      type: 'connection',
      extra: {
        eventType: event.type,
        url: stompClient.webSocket?.url,
        readyState,
        readyStateText,
      },
    });
  }

  static handleSubscriptionError({
    url,
    errorMessage,
    messageBody,
    onError,
  }: SubscriptionErrorParams): WebSocketError {
    const fullMessage = `구독 메시지 오류 (${url}): ${errorMessage}`;

    const extra: SubscriptionErrorExtra = { url, messageBody, errorMessage };

    return this.handleError(
      fullMessage,
      {
        type: 'subscription',
        extra,
      },
      onError
    );
  }

  static handleConnectionRequiredError({
    type,
    url,
    isConnected,
    hasClient,
    onError,
  }: ConnectionRequiredErrorParams): WebSocketError {
    const TYPE_MESSAGE = {
      subscription: '구독',
      send: '메시지 전송',
    } as const;

    const errorMessage = `${TYPE_MESSAGE[type]} 실패 (${url}): WebSocket 연결 안됨`;

    return this.handleError(
      errorMessage,
      {
        type,
        extra: { url, isConnected, hasClient },
      },
      onError
    );
  }

  static handleParsingError({
    url,
    originalError,
    messageBody,
    onError,
  }: ParsingErrorParams): WebSocketError {
    const errorMessage = `JSON 파싱 실패 (${url}): ${originalError instanceof Error ? originalError.message : String(originalError)}`;

    return this.handleError(
      errorMessage,
      {
        type: 'parsing',
        extra: { url, messageBody, originalError: String(originalError) },
      },
      onError
    );
  }

  static handleSendError({ url, originalError, body, onError }: SendErrorParams): WebSocketError {
    const errorMessage = `메시지 전송 중 오류 (${url}): ${originalError instanceof Error ? originalError.message : String(originalError)}`;

    return this.handleError(
      errorMessage,
      {
        type: 'send',
        extra: { url, body: String(body), originalError: String(originalError) },
      },
      onError
    );
  }
}

export default WebSocketErrorHandler;
