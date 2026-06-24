import { Client, IFrame, IStompSocket } from '@stomp/stompjs';
import { useEffect, useRef, useState } from 'react';

type StompInternalSocket = {
  _transport?: { url?: string };
} & IStompSocket;

export const useStompSessionWatcher = (client: Client | null, connectedFrame?: IFrame | null) => {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const prevSessionIdRef = useRef<string | null>(null);

  useEffect(() => {
    if (!client || !connectedFrame) return;

    const currentSessionId = extractSessionId(client);

    if (currentSessionId && currentSessionId !== prevSessionIdRef.current) {
      console.log('🔄 SessionId 변경 감지', {
        prev: prevSessionIdRef.current,
        cur: currentSessionId,
      });
      setSessionId(currentSessionId);
      prevSessionIdRef.current = currentSessionId;
    }
  }, [client, connectedFrame]);

  return { sessionId };
};

const extractSessionId = (stompClient: Client): string | null => {
  try {
    const ws = stompClient.webSocket as StompInternalSocket;
    if (!ws) return null;
    if (ws._transport?.url) {
      const match = ws._transport.url.match(/\/([a-zA-Z0-9_-]+)\/[^/]+$/);
      if (match) return match[1];
    }
    return null;
  } catch (error) {
    console.warn('⚠️ SessionId 추출 실패', error);
    return null;
  }
};
