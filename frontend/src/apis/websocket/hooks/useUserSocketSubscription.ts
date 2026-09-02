import { StompSubscription } from '@stomp/stompjs';
import { useEffect, useRef } from 'react';
import { useUserSocket } from '../contexts/UserSocketContext';
import type { WebSocketSuccess } from '../constants/constants';
import type { WsPayloadOf, WsQueuePath } from '../generated/wsContract';

export const useUserSocketSubscription = <D extends WsQueuePath>(
  destination: D,
  // 개인 소켓은 envelope 를 벗기지 않고 그대로 넘긴다(UserSocketProvider). 방 소켓과 다르다.
  onData: (data: WebSocketSuccess<WsPayloadOf<D>>) => void,
  enabled: boolean = true
) => {
  const { isConnected, subscribe } = useUserSocket();
  const subscriptionRef = useRef<StompSubscription | null>(null);
  const onDataRef = useRef(onData);

  useEffect(() => {
    onDataRef.current = onData;
  }, [onData]);

  useEffect(() => {
    if (!enabled || !isConnected) return;

    const sub = subscribe<WebSocketSuccess<WsPayloadOf<D>>>(destination, (data) =>
      onDataRef.current(data)
    );
    subscriptionRef.current = sub;

    return () => {
      subscriptionRef.current?.unsubscribe();
      subscriptionRef.current = null;
    };
  }, [destination, enabled, isConnected, subscribe]);
};
