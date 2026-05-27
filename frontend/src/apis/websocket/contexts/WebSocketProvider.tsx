import { PropsWithChildren, useCallback, useEffect, useRef, useSyncExternalStore } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  fetchRecoveryMessages,
  getLastStreamId,
  RecoveryMessage,
  saveLastStreamId,
} from '@/apis/rest/recovery';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useStompSessionWatcher } from '../hooks/useStompSessionWatcher';
import { useWebSocketConnection } from '../hooks/useWebSocketConnection';
import { useWebSocketMessaging } from '../hooks/useWebSocketMessaging';
import { useWebSocketReconnection } from '../hooks/useWebSocketReconnection';
import { subscriptionRegistry } from '../utils/subscriptionRegistry';
import { WebSocketContext, WebSocketContextType } from './WebSocketContext';

const TOPIC_PREFIX = '/topic';

const RECOVERY_INITIAL_DELAY_MS = 500;
const RECOVERY_RETRY_DELAY_MS = 300;
const RECOVERY_COMPLETE_DELAY_MS = 2000;

const SCREEN_TRANSITION_PATTERNS = {
  WINNER: '/winner',
  ROULETTE: '/roulette',
  ROUND: '/round',
} as const;

const isScreenTransitionMessage = (destination: string): boolean => {
  return Object.values(SCREEN_TRANSITION_PATTERNS).some((pattern) => destination.includes(pattern));
};

const extractSubscriptionPath = (destination: string): string => {
  return destination.replace(TOPIC_PREFIX, '');
};

// 모듈 레벨 변수로 동기적 접근 보장
let isRecoveringGlobal = false;
let listeners: Array<() => void> = [];

const setIsRecoveringGlobal = (value: boolean) => {
  isRecoveringGlobal = value;
  listeners.forEach((listener) => listener());
};

const subscribeToRecovering = (listener: () => void) => {
  listeners.push(listener);
  return () => {
    listeners = listeners.filter((l) => l !== listener);
  };
};

const getIsRecoveringSnapshot = () => isRecoveringGlobal;

// 외부에서 동기적으로 접근 가능한 함수 export
export const getIsRecovering = () => isRecoveringGlobal;

export const WebSocketProvider = ({ children }: PropsWithChildren) => {
  const navigate = useNavigate();
  const { joinCode, myName } = useIdentifier();

  const isRecovering = useSyncExternalStore(subscribeToRecovering, getIsRecoveringSnapshot);
  const recoveryTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const { client, isConnected, startSocket, stopSocket, connectedFrame } = useWebSocketConnection();
  const { sessionId } = useStompSessionWatcher(client, connectedFrame);
  const { subscribe, send } = useWebSocketMessaging({
    client,
    isConnected,
    playerName: myName,
    joinCode,
  });

  const routeRecoveryMessage = useCallback(
    (destination: string) => {
      const navOptions = { replace: true, state: { fromInternal: true } };

      if (destination.includes(SCREEN_TRANSITION_PATTERNS.WINNER)) {
        console.log('🔄 복구: 당첨자 화면으로 이동');
        navigate(`/room/${joinCode}/roulette/result`, navOptions);
        return true;
      }

      if (destination.includes(SCREEN_TRANSITION_PATTERNS.ROULETTE)) {
        console.log('🔄 복구: 룰렛 화면으로 이동');
        navigate(`/room/${joinCode}/roulette/play`, navOptions);
        return true;
      }

      if (destination.includes(SCREEN_TRANSITION_PATTERNS.ROUND)) {
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

    const lastStreamId = getLastStreamId(joinCode, myName);
    if (!lastStreamId) {
      console.log('⚠️ 복구 스킵: lastStreamId 없음');
      return;
    }

    // 동기적으로 설정
    setIsRecoveringGlobal(true);

    await new Promise((resolve) => setTimeout(resolve, RECOVERY_INITIAL_DELAY_MS));

    if (!joinCode) return;

    console.log('🔄 메시지 복구 시작:', { joinCode, myName, lastStreamId });

    const MAX_RETRY = 3;
    let messages: RecoveryMessage[] = [];

    for (let attempt = 0; attempt < MAX_RETRY; attempt++) {
      messages = await fetchRecoveryMessages(joinCode, myName, lastStreamId);

      if (messages.length > 0 || attempt === MAX_RETRY - 1) {
        break;
      }

      console.log(`🔄 복구 재시도 ${attempt + 1}/${MAX_RETRY}`);
      await new Promise((resolve) => setTimeout(resolve, RECOVERY_RETRY_DELAY_MS));
      if (!joinCode) return;
    }

    if (messages.length === 0) {
      console.log('✅ 복구할 메시지 없음');
      setIsRecoveringGlobal(false);
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

      saveLastStreamId(joinCode, myName, msg.streamId);
    }

    if (lastScreenTransitionMsg) {
      const handled = routeRecoveryMessage(lastScreenTransitionMsg.destination);

      if (!handled) {
        dispatchToSubscribers(lastScreenTransitionMsg);
      }
    }

    console.log('✅ 메시지 복구 완료');

    // 이전 타이머 정리
    if (recoveryTimeoutRef.current) {
      clearTimeout(recoveryTimeoutRef.current);
    }

    recoveryTimeoutRef.current = setTimeout(() => {
      setIsRecoveringGlobal(false);
    }, RECOVERY_COMPLETE_DELAY_MS);
  }, [joinCode, myName, routeRecoveryMessage, dispatchToSubscribers]);

  useEffect(() => {
    if (!joinCode) {
      setIsRecoveringGlobal(false);
      if (recoveryTimeoutRef.current) {
        clearTimeout(recoveryTimeoutRef.current);
        recoveryTimeoutRef.current = null;
      }
    }
  }, [joinCode]);

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
    isRecovering,
  };

  return <WebSocketContext.Provider value={contextValue}>{children}</WebSocketContext.Provider>;
};
