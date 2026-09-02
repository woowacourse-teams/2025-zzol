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
  isFriendsLoaded: boolean;
  isInRoomRef: MutableRefObject<boolean>;
};

export const useFriendSocketEvents = ({
  setFriends,
  setReceivedRequests,
  setSentRequests,
  isAuthenticated,
  isFriendsLoaded,
  isInRoomRef,
}: Actions) => {
  const { showToast } = useToast();
  const { openModal, closeModal } = useModal();

  // 친구 요청 수신
  useUserSocketSubscription(
    '/user/queue/friends/requests',
    useCallback(
      (event: FriendRequestEvent) => {
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
  useUserSocketSubscription(
    '/user/queue/friends/responses',
    useCallback(
      (event: FriendResponseEvent) => {
        const { data } = event;
        setSentRequests((prev) => prev.filter((r) => r.requestId !== data.requestId));

        if (data.accepted) {
          const newFriend: Friend = {
            userId: data.counterpartUserId,
            userCode: data.counterpartUserCode,
            nickname: data.counterpartNickname,
            since: new Date().toISOString(),
            online: false,
            joinCode: null,
            joinable: false,
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
  useUserSocketSubscription(
    '/user/queue/friends/removed',
    useCallback(
      (event: FriendRemovedEvent) => {
        setFriends((prev) => prev.filter((f) => f.userId !== event.data.removedByUserId));
      },
      [setFriends]
    ),
    isAuthenticated
  );

  // 방 초대 수신 — 방 안에 있으면 무시
  useUserSocketSubscription(
    '/user/queue/rooms/invitations',
    useCallback(
      (event: RoomInvitationEvent) => {
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

  // 친구 접속·방 참여 상태 전이 (서버가 항상 전체 스냅샷을 보낸다)
  // REST 완료(isFriendsLoaded) 후 구독 — 서버 일괄 푸시가 friends 배열에 정상 반영되도록 순서 보장
  useUserSocketSubscription(
    '/user/queue/friends/presence',
    useCallback(
      (event: FriendPresenceEvent) => {
        const { data } = event;
        setFriends((prev) =>
          prev.map((f) =>
            f.userId === data.userId
              ? { ...f, online: data.online, joinCode: data.joinCode, joinable: data.joinable }
              : f
          )
        );
      },
      [setFriends]
    ),
    isAuthenticated && isFriendsLoaded
  );
};
