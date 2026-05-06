import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { PropsWithChildren, useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { tokenStore } from '@/features/auth/tokens';
import { createUserStompClient } from '../utils/createUserStompClient';
import { UserSocketContext, UserSocketContextType } from './UserSocketContext';

export const UserSocketProvider = ({ children }: PropsWithChildren) => {
  const { isAuthenticated } = useAuth();
  const [client, setClient] = useState<Client | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  const connect = useCallback(() => {
    const token = tokenStore.getAccessToken();
    if (!token) return;

    const stompClient = createUserStompClient(token);
    stompClient.onConnect = () => {
      setIsConnected(true);
      console.log('✅ [UserSocket] 연결 성공');
    };
    stompClient.onDisconnect = () => {
      setIsConnected(false);
      console.log('❌ [UserSocket] 연결 해제');
    };
    stompClient.onStompError = (frame) => {
      setIsConnected(false);
      console.error('❌ [UserSocket] STOMP 에러', frame);
    };
    stompClient.activate();
    clientRef.current = stompClient;
    setClient(stompClient);
  }, []);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
      setClient(null);
      setIsConnected(false);
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      connect();
    } else {
      disconnect();
    }
    return disconnect;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthenticated]);

  useEffect(() => {
    const handleExpired = () => {
      disconnect();
    };
    window.addEventListener('auth:expired', handleExpired);
    return () => window.removeEventListener('auth:expired', handleExpired);
  }, [disconnect]);

  const subscribe = useCallback(
    <T,>(destination: string, onData: (data: T) => void): StompSubscription | null => {
      if (!clientRef.current || !isConnected) return null;
      return clientRef.current.subscribe(destination, (msg: IMessage) => {
        try {
          const parsed = JSON.parse(msg.body) as T;
          onData(parsed);
        } catch {
          console.error('[UserSocket] 메시지 파싱 실패', msg.body);
        }
      });
    },
    [isConnected]
  );

  // 토큰 갱신 후 새 토큰으로 STOMP 재연결
  const reconnect = useCallback(() => {
    console.log('🔄 [UserSocket] 토큰 갱신으로 인한 재연결');
    disconnect();
    connect();
  }, [disconnect, connect]);

  const contextValue: UserSocketContextType = { isConnected, client, subscribe, reconnect };

  return <UserSocketContext.Provider value={contextValue}>{children}</UserSocketContext.Provider>;
};
