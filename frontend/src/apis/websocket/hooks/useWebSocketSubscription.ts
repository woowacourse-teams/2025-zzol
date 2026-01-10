import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { usePageVisibility } from '@/hooks/usePageVisibility';
import { StompSubscription } from '@stomp/stompjs';
import { useCallback, useEffect, useRef, useState } from 'react';

export const useWebSocketSubscription = <T>(
  destination: string,
  onData: (data: T) => void,
  onError?: (error: Error) => void,
  enabled: boolean = true
) => {
  const { isVisible } = usePageVisibility();
  const { subscribe, isConnected, sessionId } = useWebSocket();

  const [isSubscribed, setIsSubscribed] = useState(false);
  const subscriptionRef = useRef<StompSubscription | null>(null);
  const prevSessionIdRef = useRef<string | null>(null);
  const retryCountRef = useRef(0);
  const retryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const unsubscribe = useCallback(() => {
    if (subscriptionRef.current) {
      try {
        subscriptionRef.current.unsubscribe();
        console.log(`🔌 구독 해제 완료: ${destination}`);
      } catch (error) {
        console.error(`❌ 구독 해제 실패: ${destination}`, error);
      } finally {
        subscriptionRef.current = null;
        setIsSubscribed(false);
      }
    }

    if (retryTimerRef.current) {
      clearTimeout(retryTimerRef.current);
      retryTimerRef.current = null;
    }
  }, [destination]);

  const trySubscribe = useCallback(() => {
    if (!enabled || !isVisible || !isConnected) {
      return;
    }

    try {
      const sub = subscribe<T>(destination, onData, onError);

      subscriptionRef.current = sub;
      prevSessionIdRef.current = sessionId;
      retryCountRef.current = 0;

      setIsSubscribed(true);

      console.log(`✅ 구독 성공: ${destination}`, { sessionId });
    } catch (error) {
      console.error(`❌ 구독 실패 (시도 ${retryCountRef.current + 1})`, error);

      const MAX_RETRY_COUNT = 5;
      const BACKOFF_BASE = 2;
      if (retryCountRef.current < MAX_RETRY_COUNT) {
        const delay = Math.min(1000 * BACKOFF_BASE ** retryCountRef.current, 10000);
        retryCountRef.current += 1;
        retryTimerRef.current = setTimeout(() => {
          console.log(`⏳ ${destination} 재시도 (${retryCountRef.current}회차)...`);
          trySubscribe();
        }, delay);
      } else {
        console.error(`🚫 ${destination} 구독 재시도 횟수 초과 (${MAX_RETRY_COUNT}회)`);
      }
    }
  }, [enabled, isVisible, isConnected, destination, onData, onError, sessionId, subscribe]);

  const doSubscribe = useCallback(() => {
    if (!sessionId) return;

    const sessionChanged = sessionId !== prevSessionIdRef.current;
    if (sessionChanged || !subscriptionRef.current) {
      if (sessionChanged) unsubscribe();
      trySubscribe();
    }
  }, [sessionId, unsubscribe, trySubscribe]);

  useEffect(() => {
    if (isConnected) doSubscribe();
    else unsubscribe();

    return unsubscribe;
  }, [isConnected, doSubscribe, unsubscribe]);

  return { isSubscribed };
};
