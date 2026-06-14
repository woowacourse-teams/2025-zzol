import { Client } from '@stomp/stompjs';
import { useCallback } from 'react';
import { WEBSOCKET_CONFIG, WebSocketMessage } from '../constants/constants';
import { saveLastStreamId } from '@/apis/rest/recovery';
import WebSocketErrorHandler from '../utils/WebSocketErrorHandler';

type Props = {
  client: Client | null;
  isConnected: boolean;
  playerName: string | null;
  joinCode: string;
};

export const useWebSocketMessaging = ({ client, isConnected, playerName, joinCode }: Props) => {
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

      // 개인 큐(/user/queue/...)는 prefix 를 그대로 두고, 그 외 토픽만 /topic 을 붙인다
      const requestUrl = url.startsWith('/user/') ? url : WEBSOCKET_CONFIG.TOPIC_PREFIX + url;

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

          if (parsedMessage.id && joinCode && playerName) {
            saveLastStreamId(joinCode, playerName, parsedMessage.id);
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
    [client, isConnected, playerName, joinCode]
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
