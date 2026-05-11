import { Client, StompSubscription } from '@stomp/stompjs';
import { createContext, useContext } from 'react';

export type UserSocketContextType = {
  isConnected: boolean;
  client: Client | null;
  subscribe: <T>(destination: string, onData: (data: T) => void) => StompSubscription | null;
  reconnect: () => void;
};

export const UserSocketContext = createContext<UserSocketContextType | null>(null);

export const useUserSocket = (): UserSocketContextType => {
  const ctx = useContext(UserSocketContext);
  if (!ctx) throw new Error('useUserSocket은 UserSocketProvider 안에서 사용해야 합니다.');
  return ctx;
};
