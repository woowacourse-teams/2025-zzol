import styled from '@emotion/styled';
import { useState } from 'react';
import { ApiError } from '@/apis/rest/error';
import Button from '@/components/@common/Button/Button';
import useToast from '@/components/@common/Toast/useToast';
import { friendsApi } from '@/features/friends/api/friendsApi';
import { useFriends } from '@/features/friends/hooks/useFriends';
import { ReceivedRequest } from '@/features/friends/types';
import { theme } from '@/styles/theme';

const getErrorCode = (err: unknown): string | undefined =>
  err instanceof ApiError ? (err.data as { errorCode?: string } | null)?.errorCode : undefined;
import FriendRow from './FriendRow';

const RequestItem = ({ request }: { request: ReceivedRequest }) => {
  const { showToast } = useToast();
  const { removeReceivedRequest, addFriend } = useFriends();
  const [loading, setLoading] = useState(false);

  const handleAccept = async () => {
    try {
      setLoading(true);
      const result = await friendsApi.acceptRequest(request.requestId);
      addFriend({
        userId: result.friendUserId,
        userCode: result.friendUserCode,
        nickname: result.friendNickname,
        since: new Date().toISOString(),
        online: false,
      });
      removeReceivedRequest(request.requestId);
      showToast({ message: `${result.friendNickname} 님과 친구가 되었습니다`, type: 'success' });
    } catch (err) {
      const code = getErrorCode(err);
      if (code === 'FRIEND_REQUEST_NOT_FOUND') {
        showToast({ message: '친구 요청을 찾을 수 없습니다', type: 'error' });
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
    try {
      setLoading(true);
      await friendsApi.rejectRequest(request.requestId);
      removeReceivedRequest(request.requestId);
    } catch (err) {
      const code = getErrorCode(err);
      if (code === 'FRIEND_REQUEST_NOT_FOUND') {
        showToast({ message: '친구 요청을 찾을 수 없습니다', type: 'error' });
      } else if (code === 'FRIEND_REQUEST_INVALID_STATE') {
        showToast({ message: '이미 처리된 요청입니다', type: 'error' });
      } else {
        showToast({ message: '거절에 실패했습니다', type: 'error' });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <FriendRow
      nickname={request.nickname}
      userCode={request.userCode}
      right={
        <>
          <Button
            variant="primary"
            onClick={handleAccept}
            isLoading={loading}
            width="60px"
            height="small"
          >
            수락
          </Button>
          <Button
            variant="secondary"
            onClick={handleReject}
            isLoading={loading}
            width="60px"
            height="small"
          >
            거절
          </Button>
        </>
      }
    />
  );
};

const ReceivedRequestsList = () => {
  const { receivedRequests } = useFriends();

  if (receivedRequests.length === 0) {
    return (
      <S.Empty>
        <S.EmptyText>받은 친구 요청이 없습니다</S.EmptyText>
      </S.Empty>
    );
  }

  return (
    <S.List>
      {receivedRequests.map((req) => (
        <S.Item key={req.requestId}>
          <RequestItem request={req} />
        </S.Item>
      ))}
    </S.List>
  );
};

export default ReceivedRequestsList;

const S = {
  List: styled.div`
    display: flex;
    flex-direction: column;
  `,

  Item: styled.div`
    border-bottom: 1px solid ${theme.color.gray[100]};
    &:last-of-type {
      border-bottom: none;
    }
  `,

  Empty: styled.div`
    padding: 48px 16px;
    text-align: center;
  `,

  EmptyText: styled.p`
    ${theme.typography.small}
    color: ${theme.color.gray[400]};
  `,
};
