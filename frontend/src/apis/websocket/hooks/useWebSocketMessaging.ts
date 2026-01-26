import { Client } from '@stomp/stompjs';
import { useCallback } from 'react';
import { WEBSOCKET_CONFIG, WebSocketMessage } from '../constants/constants';
import WebSocketErrorHandler from '../utils/WebSocketErrorHandler';

type Props = {
  client: Client | null;
  isConnected: boolean;
};

const extractJoinCodeFromDestination = (destination: string): string | null => {
  const match = destination.match(/\/room\/([^/]+)/);
  return match ? match[1] : null;
};

const saveLastStreamId = (joinCode: string, streamId: string) => {
  try {
    localStorage.setItem(`lastStreamId:${joinCode}`, streamId);
  } catch (error) {
    console.warn('lastStreamId 저장 실패:', error);
  }
};

export const useWebSocketMessaging = ({ client, isConnected }: Props) => {
  const subscribe = useCallback(
    <T>(url: string, onData: (data: T) => void, onError?: (error: Error) => void) => {
      if (!client || !isConnected) {
        WebSocketErrorHandler.handleConnectionRequiredError({
          type: 'subscription',
          url,
          isConnected,
          hasClient: !!client,
          onError,
        });
        return null;
      }

      const requestUrl = WEBSOCKET_CONFIG.TOPIC_PREFIX + url;

      return client.subscribe(requestUrl, (message) => {
        try {
          const parsedMessage = JSON.parse(message.body) as WebSocketMessage<T>;

          if (!parsedMessage.success) {
            WebSocketErrorHandler.handleSubscriptionError({
              url,
              errorMessage: parsedMessage.errorMessage,
              messageBody: message.body,
              onError,
            });
            return;
          }

          // streamId 저장 (복구용)
          if (parsedMessage.id) {
            const joinCode = extractJoinCodeFromDestination(url);
            if (joinCode) {
              saveLastStreamId(joinCode, parsedMessage.id);
            }
          }

          onData(parsedMessage.data);
        } catch (error) {
          WebSocketErrorHandler.handleParsingError({
            url,
            originalError: error,
            messageBody: message.body,
            onError,
          });
        }
      });
    },
    [client, isConnected]
  );

  const send = useCallback(
    <T>(url: string, body?: T, onError?: (error: Error) => void) => {
      if (!client || !isConnected) {
        WebSocketErrorHandler.handleConnectionRequiredError({
          type: 'send',
          url,
          isConnected,
          hasClient: !!client,
          onError,
        });
        return;
      }

      const requestUrl = WEBSOCKET_CONFIG.APP_PREFIX + url;

      try {
        const payload =
          body == null ? '' : typeof body === 'object' ? JSON.stringify(body) : String(body);

        client.publish({
          destination: requestUrl,
          body: payload,
        });
      } catch (error) {
        WebSocketErrorHandler.handleSendError({
          url,
          originalError: error,
          body,
          onError,
        });
      }
    },
    [client, isConnected]
  );

  return {
    subscribe,
    send,
  };
};
