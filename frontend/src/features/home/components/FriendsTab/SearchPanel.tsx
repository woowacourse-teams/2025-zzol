import styled from '@emotion/styled';
import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '@/apis/rest/error';
import useToast from '@/components/@common/Toast/useToast';
import { friendsApi } from '@/features/friends/api/friendsApi';
import { useFriends } from '@/features/friends/hooks/useFriends';
import { RelationStatus, SearchedUser } from '@/features/friends/types';
import { theme } from '@/styles/theme';
import FriendRow from './FriendRow';

const getErrorCode = (err: unknown): string | undefined =>
  err instanceof ApiError ? (err.data as { errorCode?: string } | null)?.errorCode : undefined;

const FRIEND_REQUEST_MESSAGES: Record<string, string> = {
  CANNOT_FRIEND_SELF: '자기 자신에게는 친구 요청을 할 수 없습니다',
  USER_NOT_FOUND: '존재하지 않는 사용자입니다',
  FRIEND_ALREADY_EXISTS: '이미 친구입니다',
  FRIEND_REQUEST_ALREADY_SENT: '이미 친구 요청을 보냈습니다',
};

type ActionProps = {
  user: SearchedUser;
  onRelationChange: (userId: number, status: RelationStatus) => void;
};

const ActionButton = ({ user, onRelationChange }: ActionProps) => {
  const { showToast } = useToast();
  const { receivedRequests, addFriend, removeReceivedRequest } = useFriends();
  const [localStatus, setLocalStatus] = useState<RelationStatus>(user.relationStatus);
  const [loading, setLoading] = useState(false);

  const incomingRequest = receivedRequests.find((r) => r.userId === user.userId);

  const handleSendRequest = async () => {
    try {
      setLoading(true);
      await friendsApi.sendFriendRequest(user.userId);
      setLocalStatus('PENDING_OUTGOING');
      onRelationChange(user.userId, 'PENDING_OUTGOING');
      showToast({ message: '친구 요청을 보냈습니다', type: 'success' });
    } catch (err) {
      const code = getErrorCode(err);
      showToast({
        message: FRIEND_REQUEST_MESSAGES[code ?? ''] ?? '친구 요청에 실패했습니다',
        type: 'error',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleAccept = async () => {
    if (!incomingRequest) return;
    try {
      setLoading(true);
      const result = await friendsApi.acceptRequest(incomingRequest.requestId);
      addFriend({
        userId: result.friendUserId,
        userCode: result.friendUserCode,
        nickname: result.friendNickname,
        since: new Date().toISOString(),
        online: false,
      });
      removeReceivedRequest(incomingRequest.requestId);
      setLocalStatus('FRIEND');
      onRelationChange(user.userId, 'FRIEND');
      showToast({ message: `${user.nickname} 님과 친구가 되었습니다`, type: 'success' });
    } catch (err) {
      const code = getErrorCode(err);
      if (code === 'FRIEND_REQUEST_NOT_FOUND') {
        showToast({ message: '친구 요청을 찾을 수 없습니다', type: 'error' });
      } else if (code === 'FRIEND_REQUEST_FORBIDDEN') {
        showToast({ message: '수락 권한이 없습니다', type: 'error' });
      } else if (code === 'FRIEND_REQUEST_INVALID_STATE') {
        showToast({ message: '이미 처리된 요청입니다', type: 'error' });
      } else {
        showToast({ message: '수락에 실패했습니다', type: 'error' });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async () => {
    if (!incomingRequest) return;
    try {
      setLoading(true);
      await friendsApi.rejectRequest(incomingRequest.requestId);
      removeReceivedRequest(incomingRequest.requestId);
      setLocalStatus('NONE');
      onRelationChange(user.userId, 'NONE');
    } catch (err) {
      const code = getErrorCode(err);
      if (code === 'FRIEND_REQUEST_NOT_FOUND') {
        showToast({ message: '친구 요청을 찾을 수 없습니다', type: 'error' });
      } else if (code === 'FRIEND_REQUEST_FORBIDDEN') {
        showToast({ message: '거절 권한이 없습니다', type: 'error' });
      } else if (code === 'FRIEND_REQUEST_INVALID_STATE') {
        showToast({ message: '이미 처리된 요청입니다', type: 'error' });
      } else {
        showToast({ message: '거절에 실패했습니다', type: 'error' });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveFriend = async () => {
    try {
      setLoading(true);
      await friendsApi.removeFriend(user.userId);
      setLocalStatus('NONE');
      onRelationChange(user.userId, 'NONE');
      showToast({ message: '친구를 삭제했습니다', type: 'info' });
    } catch (err) {
      const code = getErrorCode(err);
      if (code === 'NOT_FRIEND') {
        showToast({ message: '이미 친구 관계가 아닙니다', type: 'error' });
      } else {
        showToast({ message: '친구 삭제에 실패했습니다', type: 'error' });
      }
    } finally {
      setLoading(false);
    }
  };

  if (localStatus === 'NONE') {
    return (
      <S.AddButton onClick={handleSendRequest} disabled={loading}>
        {loading ? '...' : '+ 친구 추가'}
      </S.AddButton>
    );
  }
  if (localStatus === 'PENDING_OUTGOING') {
    return <S.StatusChip $color="gray">요청됨</S.StatusChip>;
  }
  if (localStatus === 'PENDING_INCOMING') {
    return (
      <>
        <S.AcceptButton onClick={handleAccept} disabled={loading}>
          수락
        </S.AcceptButton>
        <S.RejectButton onClick={handleReject} disabled={loading}>
          거절
        </S.RejectButton>
      </>
    );
  }
  if (localStatus === 'FRIEND') {
    return (
      <S.RemoveButton onClick={handleRemoveFriend} disabled={loading}>
        친구 끊기
      </S.RemoveButton>
    );
  }
  return null;
};

type Props = {
  results: SearchedUser[];
  loading: boolean;
};

const SearchPanel = ({ results, loading }: Props) => {
  const [localResults, setLocalResults] = useState(results);

  useEffect(() => {
    setLocalResults(results);
  }, [results]);

  const handleRelationChange = useCallback((userId: number, status: RelationStatus) => {
    setLocalResults((prev) =>
      prev.map((u) => (u.userId === userId ? { ...u, relationStatus: status } : u))
    );
  }, []);

  if (loading) {
    return (
      <S.StateBox>
        <S.StateText>검색 중...</S.StateText>
      </S.StateBox>
    );
  }
  if (localResults.length === 0) {
    return (
      <S.StateBox>
        <S.StateText>검색 결과가 없습니다</S.StateText>
      </S.StateBox>
    );
  }

  return (
    <S.Card>
      {localResults.map((user, i) => (
        <S.Row key={user.userId} $last={i === localResults.length - 1}>
          <FriendRow
            nickname={user.nickname}
            userCode={user.userCode}
            right={<ActionButton user={user} onRelationChange={handleRelationChange} />}
          />
        </S.Row>
      ))}
    </S.Card>
  );
};

export default SearchPanel;

const baseButtonStyle = `
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  padding: 6px 12px;
  transition: opacity 0.12s;
  white-space: nowrap;
  &:disabled { opacity: 0.5; cursor: default; }
`;

const S = {
  Card: styled.div`
    margin: 0 16px;
    background: ${theme.color.white};
    border: 1px solid ${theme.color.gray[100]};
    border-radius: 16px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    overflow: hidden;
  `,

  Row: styled.div<{ $last: boolean }>`
    border-bottom: ${({ $last }) => ($last ? 'none' : `1px solid ${theme.color.gray[50]}`)};
  `,

  StateBox: styled.div`
    padding: 40px 16px;
    text-align: center;
  `,

  StateText: styled.p`
    ${theme.typography.small}
    color: ${theme.color.gray[400]};
  `,

  AddButton: styled.button`
    ${baseButtonStyle}
    background: ${theme.color.point[50]};
    color: ${theme.color.point[500]};
    border: 1.5px solid ${theme.color.point[200]};
    &:hover:not(:disabled) {
      background: ${theme.color.point[100]};
    }
  `,

  AcceptButton: styled.button`
    ${baseButtonStyle}
    background: ${theme.color.point[400]};
    color: ${theme.color.white};
    &:hover:not(:disabled) {
      background: ${theme.color.point[500]};
    }
  `,

  RejectButton: styled.button`
    ${baseButtonStyle}
    background: ${theme.color.gray[100]};
    color: ${theme.color.gray[600]};
    &:hover:not(:disabled) {
      background: ${theme.color.gray[200]};
    }
  `,

  RemoveButton: styled.button`
    ${baseButtonStyle}
    background: transparent;
    color: ${theme.color.gray[400]};
    border: 1px solid ${theme.color.gray[200]};
    &:hover:not(:disabled) {
      background: ${theme.color.gray[50]};
    }
  `,

  StatusChip: styled.span<{ $color: 'gray' | 'point' }>`
    display: inline-flex;
    align-items: center;
    padding: 5px 10px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
    background: ${({ $color }) =>
      $color === 'point' ? theme.color.point[50] : theme.color.gray[100]};
    color: ${({ $color }) => ($color === 'point' ? theme.color.point[500] : theme.color.gray[400])};
  `,
};
