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
