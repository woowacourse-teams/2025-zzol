import { createContext, useContext } from 'react';
import { Friend, ReceivedRequest, SentRequest } from '../types';

export type FriendsContextType = {
  friends: Friend[];
  receivedRequests: ReceivedRequest[];
  sentRequests: SentRequest[];
  pendingReceivedCount: number;
  updateFriendOnline: (userId: number, online: boolean) => void;
  removeFriendFromStore: (userId: number) => void;
  addFriend: (friend: Friend) => void;
  addReceivedRequest: (req: ReceivedRequest) => void;
  removeReceivedRequest: (requestId: number) => void;
  removeSentRequest: (requestId: number) => void;
};

export const FriendsContext = createContext<FriendsContextType | null>(null);

export const useFriendsContext = (): FriendsContextType => {
  const ctx = useContext(FriendsContext);
  if (!ctx) throw new Error('useFriendsContext는 FriendsProvider 안에서 사용해야 합니다.');
  return ctx;
};
