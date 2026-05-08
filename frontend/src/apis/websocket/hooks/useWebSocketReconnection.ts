import { useCallback, useEffect, useRef } from 'react';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { usePageVisibility } from '@/hooks/usePageVisibility';

type Props = {
  isConnected: boolean;
  startSocket: (roomToken: string) => void;
  stopSocket: () => void;
  onReconnected?: () => void;
};

export const useWebSocketReconnection = ({
  isConnected,
  startSocket,
  stopSocket,
  onReconnected,
}: Props) => {
  const { isVisible } = usePageVisibility();
  const { roomSessionToken } = useIdentifier();
  const reconnectTimerRef = useRef<number | null>(null);
  const wasBackgrounded = useRef(false);
  const hasCheckedRefresh = useRef(false);
  const wasDisconnectedRef = useRef(false);

  const clearReconnectTimer = useCallback(() => {
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
  }, []);

  const scheduleReconnect = useCallback(() => {
    clearReconnectTimer();
    wasDisconnectedRef.current = true;
    reconnectTimerRef.current = window.setTimeout(() => {
      if (roomSessionToken) startSocket(roomSessionToken);
    }, 200);
  }, [roomSessionToken, startSocket, clearReconnectTimer]);

  useEffect(() => {
    if (isConnected && wasDisconnectedRef.current) {
      console.log('🔄 재연결 완료 - 복구 시작');
      wasDisconnectedRef.current = false;
      onReconnected?.();
    }
  }, [isConnected, onReconnected]);

  useEffect(() => {
    if (hasCheckedRefresh.current) return;

    let isReload = false;

    try {
      const navigationEntries = performance.getEntriesByType(
        'navigation'
      ) as PerformanceNavigationTiming[];
      isReload = navigationEntries.length > 0 && navigationEntries[0].type === 'reload';
    } catch (error) {
      console.warn('performance.getEntriesByType not supported:', error);
      isReload = document.referrer === window.location.href;
    }

    if (isReload && !isConnected && roomSessionToken && startSocket) {
      console.log('🔄 새로고침 감지 - 웹소켓 재연결 시도');
      hasCheckedRefresh.current = true;
      wasDisconnectedRef.current = true;
      startSocket(roomSessionToken);
    }
  }, [roomSessionToken, isConnected, startSocket]);

  useEffect(() => {
    if (!isVisible && isConnected) {
      console.log('📱 백그라운드 전환 - 소켓 연결 해제');
      wasBackgrounded.current = true;
      stopSocket();
      return;
    }

    if (isVisible && !isConnected && wasBackgrounded.current) {
      wasBackgrounded.current = false;
      console.log('📱 포그라운드 복귀 - 소켓 재연결');
      scheduleReconnect();
    }

    return () => clearReconnectTimer();
  }, [isVisible, isConnected, stopSocket, scheduleReconnect, clearReconnectTimer]);

  useEffect(() => {
    const handleOnline = () => {
      if (!isConnected) {
        console.log('🌐 온라인 감지 - 소켓 재연결');
        scheduleReconnect();
      }
    };
    const handleOffline = () => {
      if (isConnected) {
        console.log('🌐 오프라인 감지 - 소켓 연결 해제');
        stopSocket();
      }
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      clearReconnectTimer();
    };
  }, [isConnected, stopSocket, scheduleReconnect, clearReconnectTimer]);
};
