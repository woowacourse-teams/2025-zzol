import { ApiError, NetworkError } from '@/apis/rest/error';
import * as Sentry from '@sentry/react';
import { WebSocketErrorOptions } from '../websocket/constants/constants';

export const reportApiError = (error: ApiError | NetworkError) => {
  if (error instanceof ApiError) {
    Sentry.captureException(error, {
      level: 'error',
      tags: {
        errorType: 'api',
        statusCode: error.status.toString(),
        errorCategory: error.status >= 500 ? 'serverError' : 'clientError',
      },
      extra: {
        timestamp: new Date().toISOString(),
        errorData: error.data,
      },
    });
  } else if (error instanceof NetworkError) {
    Sentry.captureException(error, {
      level: 'error',
      tags: {
        errorType: 'network',
        errorCategory: 'connectionError',
      },
      extra: {
        timestamp: new Date().toISOString(),
      },
    });
  }
};

export const reportWebSocketError = (errorMessage: string, options?: WebSocketErrorOptions) => {
  const { type = 'connection', extra } = options || {};

  Sentry.captureException(new Error(errorMessage), {
    level: 'error',
    tags: {
      errorType: 'websocket',
      websocketType: type,
      errorCategory: 'realtimeError',
    },
    extra: {
      timestamp: new Date().toISOString(),
      ...extra,
    },
  });
};
