import { PropsWithChildren, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { fetchRecoveryMessages, getLastStreamId, RecoveryMessage } from '@/apis/rest/recovery';
import { useStompSessionWatcher } from '../hooks/useStompSessionWatcher';
import { useWebSocketConnection } from '../hooks/useWebSocketConnection';
import { useWebSocketMessaging } from '../hooks/useWebSocketMessaging';
import { useWebSocketReconnection } from '../hooks/useWebSocketReconnection';
import { WebSocketContext, WebSocketContextType } from './WebSocketContext';

export const WebSocketProvider = ({ children }: PropsWithChildren) => {
  const navigate = useNavigate();
  const { joinCode, myName } = useIdentifier();

  const { client, isConnected, startSocket, stopSocket, connectedFrame } = useWebSocketConnection();
  const { sessionId } = useStompSessionWatcher(client, connectedFrame);
  const { subscribe, send } = useWebSocketMessaging({ client, isConnected });

  /**
   * destination별 복구 메시지 처리
   */
  const routeRecoveryMessage = useCallback(
    (msg: RecoveryMessage) => {
      const { destination } = msg;

      // 룰렛 화면 전환
      if (destination.includes('/roulette') && !destination.includes('/winner')) {
        console.log('🔄 복구: 룰렛 화면으로 이동');
        navigate(`/room/${joinCode}/roulette/play`, { replace: true });
        return;
      }

      // 당첨자 발표 화면
      if (destination.includes('/winner')) {
        console.log('🔄 복구: 당첨자 화면으로 이동');
        navigate(`/room/${joinCode}/roulette/result`, { replace: true });
        return;
      }
    },
    [joinCode, navigate]
  );

  /**
   * 재연결 시 놓친 메시지 복구
   */
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

    console.log('🔄 메시지 복구 시작:', { joinCode, myName, lastStreamId });

    const messages = await fetchRecoveryMessages(joinCode, myName, lastStreamId);

    if (messages.length === 0) {
      console.log('✅ 복구할 메시지 없음');
      return;
    }

    console.log(`🔄 복구 메시지 ${messages.length}개 처리`);

    for (const msg of messages) {
      routeRecoveryMessage(msg);

      try {
        localStorage.setItem(`lastStreamId:${joinCode}`, msg.streamId);
      } catch {
        // ignore
      }
    }

    console.log('✅ 메시지 복구 완료');
  }, [joinCode, myName, routeRecoveryMessage]);

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
