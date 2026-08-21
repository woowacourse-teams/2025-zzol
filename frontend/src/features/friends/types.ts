export type RelationStatus = 'NONE' | 'PENDING_OUTGOING' | 'PENDING_INCOMING' | 'FRIEND' | 'SELF';

export type SearchedUser = {
  userId: number;
  userCode: string;
  nickname: string;
  relationStatus: RelationStatus;
  online: boolean;
};

export type Friend = {
  userId: number;
  userCode: string;
  nickname: string;
  since: string;
  online: boolean;
  /** 참여 중인 방의 참여 코드. 어느 방에도 없으면 null */
  joinCode: string | null;
  /** 그 방에 지금 입장할 수 있는지 (로비 상태 + 정원 여유) */
  joinable: boolean;
};

export type ReceivedRequest = {
  requestId: number;
  userId: number;
  userCode: string;
  nickname: string;
  createdAt: string | number;
};

export type SentRequest = {
  requestId: number;
  userId: number;
  userCode: string;
  nickname: string;
  createdAt: string | number;
};

export type FriendRequestEvent = {
  success: boolean;
  data: {
    requestId: number;
    fromUserId: number;
    fromUserCode: string;
    fromNickname: string;
    createdAt: string | number;
  };
};

export type FriendResponseEvent = {
  success: boolean;
  data: {
    requestId: number;
    accepted: boolean;
    counterpartUserId: number;
    counterpartUserCode: string;
    counterpartNickname: string;
  };
};

export type FriendRemovedEvent = {
  success: boolean;
  data: {
    removedByUserId: number;
  };
};

export type RoomInvitationEvent = {
  success: boolean;
  data: {
    inviterUserId: number;
    inviterNickname: string;
    joinCode: string;
  };
};

export type FriendPresenceEvent = {
  success: boolean;
  data: {
    userId: number;
    online: boolean;
    joinCode: string | null;
    joinable: boolean;
  };
};
