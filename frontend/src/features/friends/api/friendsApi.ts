import { api } from '@/apis/rest/api';
import { Friend, ReceivedRequest, SearchedUser, SentRequest } from '../types';

type SendFriendRequestResponse = {
  requestId: number;
  targetUserId: number;
  status: string;
  createdAt: string;
};

type AcceptRequestResponse = {
  friendUserId: number;
  friendUserCode: string;
  friendNickname: string;
};

export const friendsApi = {
  searchByUserCode: (userCode: string) =>
    api.get<SearchedUser[]>(`/users/search?userCode=${encodeURIComponent(userCode)}`),

  searchByNickname: (nickname: string) =>
    api.get<SearchedUser[]>(`/users/search?nickname=${encodeURIComponent(nickname)}`),

  sendFriendRequest: (targetUserId: number) =>
    api.post<SendFriendRequestResponse, { targetUserId: number }>('/users/me/friends/requests', {
      targetUserId,
    }),

  getReceivedRequests: () => api.get<ReceivedRequest[]>('/users/me/friends/requests/received'),

  getSentRequests: () => api.get<SentRequest[]>('/users/me/friends/requests/sent'),

  acceptRequest: (requestId: number) =>
    api.post<AcceptRequestResponse, undefined>(
      `/users/me/friends/requests/${requestId}/accept`,
      undefined
    ),

  rejectRequest: (requestId: number) =>
    api.post<void, undefined>(`/users/me/friends/requests/${requestId}/reject`, undefined),

  getFriends: () => api.get<Friend[]>('/users/me/friends'),

  removeFriend: (friendUserId: number) => api.delete<void>(`/users/me/friends/${friendUserId}`),

  inviteToRoom: (joinCode: string, targetUserId: number) =>
    api.post<void, { targetUserId: number }>(`/rooms/${joinCode}/invitations`, { targetUserId }),
};
