import { PropsWithChildren, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchRecoveryMessages, getLastStreamId, RecoveryMessage } from '@/apis/rest/recovery';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useStompSessionWatcher } from '../hooks/useStompSessionWatcher';
import { useWebSocketConnection } from '../hooks/useWebSocketConnection';
import { useWebSocketMessaging } from '../hooks/useWebSocketMessaging';
import { useWebSocketReconnection } from '../hooks/useWebSocketReconnection';
import { subscriptionRegistry } from '../utils/subscriptionRegistry';
import { WebSocketContext, WebSocketContextType } from './WebSocketContext';

const TOPIC_PREFIX = '/topic';

const SCREEN_TRANSITION_PATTERNS = ['/roulette', '/winner', '/round'] as const;

const isScreenTransitionMessage = (destination: string): boolean => {
  return SCREEN_TRANSITION_PATTERNS.some((pattern) => destination.includes(pattern));
};

const extractSubscriptionPath = (destination: string): string => {
  return destination.replace(TOPIC_PREFIX, '');
};

export const WebSocketProvider = ({ children }: PropsWithChildren) => {
  const navigate = useNavigate();
  const { joinCode, myName } = useIdentifier();

  const { client, isConnected, startSocket, stopSocket, connectedFrame } = useWebSocketConnection();
  const { sessionId } = useStompSessionWatcher(client, connectedFrame);
  const { subscribe, send } = useWebSocketMessaging({ client, isConnected });

  const routeRecoveryMessage = useCallback(
    (destination: string) => {
      if (destination.includes('/roulette') && !destination.includes('/winner')) {
        console.log('🔄 복구: 룰렛 화면으로 이동');
        navigate(`/room/${joinCode}/roulette/play`, { replace: true });
        return true;
      }

      if (destination.includes('/winner')) {
        console.log('🔄 복구: 당첨자 화면으로 이동');
        navigate(`/room/${joinCode}/roulette/result`, { replace: true });
        return true;
      }

      if (destination.includes('/round')) {
        console.log('🔄 복구: 게임 시작 - 핸들러에게 위임');
        return false;
      }

      return false;
    },
    [joinCode, navigate]
  );

  const dispatchToSubscribers = useCallback((msg: RecoveryMessage) => {
    const { destination, response } = msg;
    const subscriptionPath = extractSubscriptionPath(destination);

    if (response.success && response.data !== null) {
      const dispatched = subscriptionRegistry.dispatch(subscriptionPath, response.data);
      if (dispatched) {
        console.log('🔄 복구 메시지 핸들러에 전달:', subscriptionPath);
      }
      return dispatched;
    }
    return false;
  }, []);

  const handleReconnected = useCallback(async () => {
    if (!joinCode || !myName) {
      console.log('⚠️ 복구 스킵: joinCode 또는 myName 없음');
      return;
    }

    const lastStreamId = getLastStreamId(joinCode);
    if (!lastStreamId) {
      console.log('⚠️ 복구 스킵: lastStreamId 없음');
      return;
    }

    await new Promise((resolve) => setTimeout(resolve, 500));

    console.log('🔄 메시지 복구 시작:', { joinCode, myName, lastStreamId });

    const MAX_RETRY = 3;
    let messages: RecoveryMessage[] = [];

    for (let attempt = 0; attempt < MAX_RETRY; attempt++) {
      messages = await fetchRecoveryMessages(joinCode, myName, lastStreamId);

      if (messages.length > 0 || attempt === MAX_RETRY - 1) {
        break;
      }

      console.log(`🔄 복구 재시도 ${attempt + 1}/${MAX_RETRY}`);
      await new Promise((resolve) => setTimeout(resolve, 300));
    }

    if (messages.length === 0) {
      console.log('✅ 복구할 메시지 없음');
      return;
    }

    console.log(`🔄 복구 메시지 ${messages.length}개 처리`);

    let lastScreenTransitionMsg: RecoveryMessage | null = null;

    for (const msg of messages) {
      const { destination } = msg;

      if (isScreenTransitionMessage(destination)) {
        lastScreenTransitionMsg = msg;
      } else {
        dispatchToSubscribers(msg);
      }

      try {
        localStorage.setItem(`lastStreamId:${joinCode}`, msg.streamId);
      } catch {
        // ignore
      }
    }

    if (lastScreenTransitionMsg) {
      const handled = routeRecoveryMessage(lastScreenTransitionMsg.destination);

      if (!handled) {
        dispatchToSubscribers(lastScreenTransitionMsg);
      }
    }

    console.log('✅ 메시지 복구 완료');
  }, [joinCode, myName, routeRecoveryMessage, dispatchToSubscribers]);

  useWebSocketReconnection({
    isConnected,
    startSocket,
    stopSocket,
    onReconnected: handleReconnected,
  });

  const contextValue: WebSocketContextType = {
    startSocket,
    stopSocket,
    subscribe,
    send,
    isConnected,
    client,
    sessionId,
  };

  return <WebSocketContext.Provider value={contextValue}>{children}</WebSocketContext.Provider>;
};
