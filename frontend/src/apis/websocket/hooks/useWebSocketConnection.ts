import { Client, IFrame } from '@stomp/stompjs';
import { useCallback, useState } from 'react';
import { createStompClient } from '../utils/createStompClient';
import WebSocketErrorHandler from '../utils/WebSocketErrorHandler';

export const useWebSocketConnection = () => {
  const [client, setClient] = useState<Client | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [connectedFrame, setConnectedFrame] = useState<IFrame | null>(null);

  const handleConnect = useCallback((frame: IFrame) => {
    setIsConnected(true);
    setConnectedFrame(frame);
    console.log('✅ WebSocket 연결 성공', { frame });
  }, []);

  const handleDisconnect = useCallback(() => {
    setIsConnected(false);
    setConnectedFrame(null);
    console.log('❌ WebSocket 연결 해제');
  }, []);

  const handleStompError = useCallback((frame: IFrame) => {
    WebSocketErrorHandler.handleStompError(frame);
    setIsConnected(false);
    setConnectedFrame(null);
    console.error('❌ StompError 발생');
  }, []);

  const handleWebSocketError = useCallback((event: Event, stompClient: Client) => {
    WebSocketErrorHandler.handleWebSocketError(event, stompClient);
    setIsConnected(false);
    setConnectedFrame(null);
    console.error('❌ WebSocketError 발생');
  }, []);

  const setupStompClient = useCallback(
    (joinCode: string, myName: string): Client => {
      const stompClient = createStompClient({ joinCode, playerName: myName });
      stompClient.onConnect = (frame) => handleConnect(frame);
      stompClient.onDisconnect = handleDisconnect;
      stompClient.onStompError = handleStompError;
      stompClient.onWebSocketError = (event) => handleWebSocketError(event, stompClient);
      return stompClient;
    },
    [handleConnect, handleDisconnect, handleStompError, handleWebSocketError]
  );

  const validateClient = useCallback(() => {
    if (client && isConnected) {
      console.log('⚠️ 이미 연결된 클라이언트가 있습니다.');
      return false;
    }
    if (client && !isConnected) {
      console.log('🧹 이전 클라이언트 정리 중...');
      client.deactivate();
      setClient(null);
    }
    return true;
  }, [client, isConnected]);

  const validateConnectionParams = useCallback((joinCode: string, myName: string) => {
    if (!joinCode || !myName) {
      console.error('❌ WebSocket 연결 실패: 참여코드 또는 이름이 없습니다.', {
        joinCode,
        myName,
      });
      return false;
    }
    return true;
  }, []);

  const startSocket = useCallback(
    (joinCode: string, myName: string) => {
      if (!validateClient() || !validateConnectionParams(joinCode, myName)) return;
      console.log('🚀 WebSocket 연결 시작', { joinCode, myName });
      const stompClient = setupStompClient(joinCode, myName);
      setClient(stompClient);
      stompClient.activate();
    },
    [validateClient, validateConnectionParams, setupStompClient]
  );

  const stopSocket = useCallback(() => {
    if (!client) return;
    console.log('🛑 WebSocket 연결 종료...');
    client.deactivate();
    setIsConnected(false);
    setConnectedFrame(null);
    setClient(null);
  }, [client]);

  return {
    client,
    isConnected,
    startSocket,
    stopSocket,
    connectedFrame,
  };
};
