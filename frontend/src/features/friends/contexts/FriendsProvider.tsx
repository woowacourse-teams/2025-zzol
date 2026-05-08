import { PropsWithChildren, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useUserSocket } from '@/apis/websocket/contexts/UserSocketContext';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { tokenStore } from '@/features/auth/tokens';
import { friendsApi } from '../api/friendsApi';
import { useFriendSocketEvents } from '../hooks/useFriendSocketEvents';
import { Friend, ReceivedRequest, SentRequest } from '../types';
import { FriendsContext, FriendsContextType } from './FriendsContext';

export const FriendsProvider = ({ children }: PropsWithChildren) => {
  const { isAuthenticated } = useAuth();
  const { reconnect } = useUserSocket();
  const { pathname } = useLocation();

  const [friends, setFriends] = useState<Friend[]>([]);
  const [receivedRequests, setReceivedRequests] = useState<ReceivedRequest[]>([]);
  const [sentRequests, setSentRequests] = useState<SentRequest[]>([]);

  // ref로 관리해 deps 없이 최신 값 참조
  const isInRoomRef = useRef(false);
  const reconnectRef = useRef(reconnect);

  useEffect(() => {
    isInRoomRef.current = pathname.startsWith('/room/');
  }, [pathname]);

  useEffect(() => {
    reconnectRef.current = reconnect;
  }, [reconnect]);

  // 로그인 시 초기 데이터 로드
  useEffect(() => {
    if (!isAuthenticated) {
      setFriends([]);
      setReceivedRequests([]);
      setSentRequests([]);
      return;
    }

    const load = async () => {
      try {
        const tokenBefore = tokenStore.getAccessToken();
        const [friendsList, received, sent] = await Promise.all([
          friendsApi.getFriends(),
          friendsApi.getReceivedRequests(),
          friendsApi.getSentRequests(),
        ]);
        setFriends(friendsList);
        setReceivedRequests(received);
        setSentRequests(sent);
        if (tokenBefore !== tokenStore.getAccessToken()) {
          reconnectRef.current();
        }
      } catch {
        // 초기 로드 실패 시 조용히 빈 상태 유지
      }
    };

    load();
  }, [isAuthenticated]);

  // STOMP 이벤트 처리 — 별도 훅으로 분리
  useFriendSocketEvents({
    setFriends,
    setReceivedRequests,
    setSentRequests,
    isAuthenticated,
    isInRoomRef,
  });

  const updateFriendOnline = useCallback((userId: number, online: boolean) => {
    setFriends((prev) => prev.map((f) => (f.userId === userId ? { ...f, online } : f)));
  }, []);

  const removeFriendFromStore = useCallback((userId: number) => {
    setFriends((prev) => prev.filter((f) => f.userId !== userId));
  }, []);

  const addFriend = useCallback((friend: Friend) => {
    setFriends((prev) => (prev.some((f) => f.userId === friend.userId) ? prev : [...prev, friend]));
  }, []);

  const addReceivedRequest = useCallback((req: ReceivedRequest) => {
    setReceivedRequests((prev) => [req, ...prev]);
  }, []);

  const removeReceivedRequest = useCallback((requestId: number) => {
    setReceivedRequests((prev) => prev.filter((r) => r.requestId !== requestId));
  }, []);

  const removeSentRequest = useCallback((requestId: number) => {
    setSentRequests((prev) => prev.filter((r) => r.requestId !== requestId));
  }, []);

  const contextValue: FriendsContextType = useMemo(
    () => ({
      friends,
      receivedRequests,
      sentRequests,
      pendingReceivedCount: receivedRequests.length,
      updateFriendOnline,
      removeFriendFromStore,
      addFriend,
      addReceivedRequest,
      removeReceivedRequest,
      removeSentRequest,
    }),
    [
      friends,
      receivedRequests,
      sentRequests,
      updateFriendOnline,
      removeFriendFromStore,
      addFriend,
      addReceivedRequest,
      removeReceivedRequest,
      removeSentRequest,
    ]
  );

  return <FriendsContext.Provider value={contextValue}>{children}</FriendsContext.Provider>;
};
