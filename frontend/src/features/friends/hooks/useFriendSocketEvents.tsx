import { Dispatch, MutableRefObject, SetStateAction, useCallback } from 'react';
import { useUserSocketSubscription } from '@/apis/websocket/hooks/useUserSocketSubscription';
import useModal from '@/components/@common/Modal/useModal';
import useToast from '@/components/@common/Toast/useToast';
import RoomInvitationModal from '../components/RoomInvitationModal';
import {
  Friend,
  FriendPresenceEvent,
  FriendRemovedEvent,
  FriendRequestEvent,
  FriendResponseEvent,
  ReceivedRequest,
  RoomInvitationEvent,
} from '../types';

type Actions = {
  setFriends: Dispatch<SetStateAction<Friend[]>>;
  setReceivedRequests: Dispatch<SetStateAction<ReceivedRequest[]>>;
  setSentRequests: Dispatch<SetStateAction<ReceivedRequest[]>>;
  isAuthenticated: boolean;
  isInRoomRef: MutableRefObject<boolean>;
};

export const useFriendSocketEvents = ({
  setFriends,
  setReceivedRequests,
  setSentRequests,
  isAuthenticated,
  isInRoomRef,
}: Actions) => {
  const { showToast } = useToast();
  const { openModal, closeModal } = useModal();

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
      [setReceivedRequests, showToast]
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
      [setFriends, setSentRequests, showToast]
    ),
    isAuthenticated
  );

  // 친구 끊기 알림 수신
  useUserSocketSubscription<FriendRemovedEvent>(
    '/user/queue/friends/removed',
    useCallback(
      (event) => {
        setFriends((prev) => prev.filter((f) => f.userId !== event.data.removedByUserId));
      },
      [setFriends]
    ),
    isAuthenticated
  );

  // 방 초대 수신 — 방 안에 있으면 무시
  useUserSocketSubscription<RoomInvitationEvent>(
    '/user/queue/rooms/invitations',
    useCallback(
      (event) => {
        if (isInRoomRef.current) return;
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
      [isInRoomRef, openModal, closeModal]
    ),
    isAuthenticated
  );

  // 친구 온라인/오프라인 전이
  useUserSocketSubscription<FriendPresenceEvent>(
    '/user/queue/friends/presence',
    useCallback(
      (event) => {
        const { data } = event;
        setFriends((prev) =>
          prev.map((f) => (f.userId === data.userId ? { ...f, online: data.online } : f))
        );
      },
      [setFriends]
    ),
    isAuthenticated
  );
};
