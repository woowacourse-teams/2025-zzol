import styled from '@emotion/styled';
import { useFriends } from '@/features/friends/hooks/useFriends';
import { theme } from '@/styles/theme';
import FriendRow from './FriendRow';

const SentRequestsList = () => {
  const { sentRequests } = useFriends();

  if (sentRequests.length === 0) {
    return (
      <S.Empty>
        <S.EmptyText>보낸 친구 요청이 없습니다</S.EmptyText>
      </S.Empty>
    );
  }

  return (
    <S.List>
      {sentRequests.map((req) => (
        <S.Item key={req.requestId}>
          <FriendRow
            nickname={req.nickname}
            userCode={req.userCode}
            right={<S.PendingLabel>대기 중</S.PendingLabel>}
          />
        </S.Item>
      ))}
    </S.List>
  );
};

export default SentRequestsList;

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

  PendingLabel: styled.span`
    ${theme.typography.small}
    color: ${theme.color.gray[400]};
    background: ${theme.color.gray[100]};
    padding: 4px 10px;
    border-radius: 20px;
  `,
};
