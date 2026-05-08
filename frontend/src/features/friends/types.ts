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
  };
};
