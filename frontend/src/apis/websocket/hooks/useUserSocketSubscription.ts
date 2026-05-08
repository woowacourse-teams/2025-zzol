import { StompSubscription } from '@stomp/stompjs';
import { useEffect, useRef } from 'react';
import { useUserSocket } from '../contexts/UserSocketContext';

export const useUserSocketSubscription = <T>(
  destination: string,
  onData: (data: T) => void,
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

    const sub = subscribe<T>(destination, (data) => onDataRef.current(data));
    subscriptionRef.current = sub;

    return () => {
      subscriptionRef.current?.unsubscribe();
      subscriptionRef.current = null;
    };
  }, [destination, enabled, isConnected, subscribe]);
};
