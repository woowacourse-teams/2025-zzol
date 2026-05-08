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
  const { reconnect, isConnected } = useUserSocket();
  const { pathname } = useLocation();

  const [friends, setFriends] = useState<Friend[]>([]);
  const [receivedRequests, setReceivedRequests] = useState<ReceivedRequest[]>([]);
  const [sentRequests, setSentRequests] = useState<SentRequest[]>([]);
  const [isFriendsLoaded, setIsFriendsLoaded] = useState(false);

  // ref로 관리해 deps 없이 최신 값 참조
  const isInRoomRef = useRef(false);
  const reconnectRef = useRef(reconnect);
  // STOMP 연결 후 1회만 초기 로드 (재연결 시 중복 로드 방지)
  const initialLoadDoneRef = useRef(false);

  useEffect(() => {
    isInRoomRef.current = pathname.startsWith('/room/');
  }, [pathname]);

  useEffect(() => {
    reconnectRef.current = reconnect;
  }, [reconnect]);

  // 로그인 해제 시 상태 초기화
  useEffect(() => {
    if (!isAuthenticated) {
      setFriends([]);
      setReceivedRequests([]);
      setSentRequests([]);
      setIsFriendsLoaded(false);
      initialLoadDoneRef.current = false;
    }
  }, [isAuthenticated]);

  // STOMP 연결 완료 후 초기 로드 — presence 이벤트 유실 방지
  // (구독 전에 REST를 호출하면 presence 이벤트가 빈 friends 배열에 적용되어 소실됨)
  useEffect(() => {
    if (!isAuthenticated || !isConnected || initialLoadDoneRef.current) return;

    initialLoadDoneRef.current = true;

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
        setIsFriendsLoaded(true);
        if (tokenBefore !== tokenStore.getAccessToken()) {
          reconnectRef.current();
        }
      } catch {
        // 초기 로드 실패 시 조용히 빈 상태 유지
      }
    };

    load();
  }, [isAuthenticated, isConnected]);

  // STOMP 이벤트 처리 — 별도 훅으로 분리
  // presence 구독은 REST 완료(isFriendsLoaded) 후 활성화
  useFriendSocketEvents({
    setFriends,
    setReceivedRequests,
    setSentRequests,
    isAuthenticated,
    isFriendsLoaded,
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
