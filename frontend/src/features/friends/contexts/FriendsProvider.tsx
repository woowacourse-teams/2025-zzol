import { PropsWithChildren, useCallback, useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import useModal from '@/components/@common/Modal/useModal';
import useToast from '@/components/@common/Toast/useToast';
import { useUserSocketSubscription } from '@/apis/websocket/hooks/useUserSocketSubscription';
import { useUserSocket } from '@/apis/websocket/contexts/UserSocketContext';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { tokenStore } from '@/features/auth/tokens';
import { friendsApi } from '../api/friendsApi';
import RoomInvitationModal from '../components/RoomInvitationModal';
import {
  Friend,
  FriendPresenceEvent,
  FriendRemovedEvent,
  FriendRequestEvent,
  FriendResponseEvent,
  ReceivedRequest,
  RoomInvitationEvent,
  SentRequest,
} from '../types';
import { FriendsContext, FriendsContextType } from './FriendsContext';

export const FriendsProvider = ({ children }: PropsWithChildren) => {
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const { openModal, closeModal } = useModal();
  const { reconnect } = useUserSocket();
  const { pathname } = useLocation();
  const isInRoom = pathname.startsWith('/room/');

  const [friends, setFriends] = useState<Friend[]>([]);
  const [receivedRequests, setReceivedRequests] = useState<ReceivedRequest[]>([]);
  const [sentRequests, setSentRequests] = useState<SentRequest[]>([]);

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
        // 토큰 만료 감지: 백엔드는 만료 토큰으로도 STOMP 연결을 허용(sessionId fallback)하므로
        // REST 호출 결과로만 실제 인증 상태를 확인할 수 있다.
        // 갱신 전 토큰을 저장해 두고, 성공 후 비교해 갱신 여부를 판단한다.
        const tokenBefore = tokenStore.getAccessToken();
        const [friendsList, received, sent] = await Promise.all([
          friendsApi.getFriends(),
          friendsApi.getReceivedRequests(),
          friendsApi.getSentRequests(),
        ]);
        setFriends(friendsList);
        setReceivedRequests(received);
        setSentRequests(sent);
        // 토큰이 갱신됐으면 STOMP도 새 토큰으로 재연결
        if (tokenBefore !== tokenStore.getAccessToken()) {
          reconnect();
        }
      } catch {
        // 초기 로드 실패 시 조용히 빈 상태 유지
      }
    };

    load();
  }, [isAuthenticated, reconnect]);

  // 친구 요청 수신
  useUserSocketSubscription<FriendRequestEvent>(
    '/user/queue/friends/requests',
    useCallback(
      (event) => {
        const { data } = event;
        const req: ReceivedRequest = {
          requestId: data.requestId,
          userId: data.fromUserId,
          userCode: data.fromUserCode,
          nickname: data.fromNickname,
          createdAt: data.createdAt,
        };
        setReceivedRequests((prev) => [req, ...prev]);
        showToast({
          message: `${req.nickname ?? '누군가'} 님이 친구 요청을 보냈습니다`,
          type: 'info',
        });
      },
      [showToast]
    ),
    isAuthenticated
  );

  // 내가 보낸 요청 응답 수신
  useUserSocketSubscription<FriendResponseEvent>(
    '/user/queue/friends/responses',
    useCallback(
      (event) => {
        const { data } = event;
        setSentRequests((prev) => prev.filter((r) => r.requestId !== data.requestId));

        if (data.accepted) {
          const newFriend: Friend = {
            userId: data.counterpartUserId,
            userCode: data.counterpartUserCode,
            nickname: data.counterpartNickname,
            since: new Date().toISOString(),
            online: false,
          };
          setFriends((prev) =>
            prev.some((f) => f.userId === newFriend.userId) ? prev : [...prev, newFriend]
          );
          showToast({
            message: `${data.counterpartNickname} 님과 친구가 되었습니다`,
            type: 'success',
          });
        } else {
          showToast({ message: '친구 요청이 거절되었습니다', type: 'info' });
        }
      },
      [showToast]
    ),
    isAuthenticated
  );

  // 친구 끊기 알림 수신
  useUserSocketSubscription<FriendRemovedEvent>(
    '/user/queue/friends/removed',
    useCallback((event) => {
      setFriends((prev) => prev.filter((f) => f.userId !== event.data.removedByUserId));
    }, []),
    isAuthenticated
  );

  // 방 초대 수신
  useUserSocketSubscription<RoomInvitationEvent>(
    '/user/queue/rooms/invitations',
    useCallback(
      (event) => {
        if (isInRoom) return;
        const { data } = event;
        openModal(
          <RoomInvitationModal
            inviterNickname={data.inviterNickname}
            joinCode={data.joinCode}
            onClose={closeModal}
          />,
          { title: '방 초대', showCloseButton: false }
        );
      },
      [isInRoom, openModal, closeModal]
    ),
    isAuthenticated
  );

  // 친구 온라인/오프라인 전이
  useUserSocketSubscription<FriendPresenceEvent>(
    '/user/queue/friends/presence',
    useCallback((event) => {
      setFriends((prev) =>
        prev.map((f) => (f.userId === event.userId ? { ...f, online: event.online } : f))
      );
    }, []),
    isAuthenticated
  );

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

  const contextValue: FriendsContextType = {
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
  };

  return <FriendsContext.Provider value={contextValue}>{children}</FriendsContext.Provider>;
};
