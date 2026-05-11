import styled from '@emotion/styled';
import { useState } from 'react';
import { ApiError } from '@/apis/rest/error';
import Button from '@/components/@common/Button/Button';
import useModal from '@/components/@common/Modal/useModal';
import useToast from '@/components/@common/Toast/useToast';
import { friendsApi } from '@/features/friends/api/friendsApi';
import { useFriends } from '@/features/friends/hooks/useFriends';
import { Friend } from '@/features/friends/types';
import { theme } from '@/styles/theme';
import FriendRow from './FriendRow';

const getErrorCode = (err: unknown): string | undefined =>
  err instanceof ApiError ? (err.data as { errorCode?: string } | null)?.errorCode : undefined;

const RemoveConfirm = ({
  friend,
  onConfirm,
  onCancel,
}: {
  friend: Friend;
  onConfirm: () => void;
  onCancel: () => void;
}) => (
  <S.ConfirmBox>
    <S.ConfirmText>
      <strong>{friend.nickname}</strong> 님을 친구 목록에서 삭제할까요?
    </S.ConfirmText>
    <S.ConfirmButtons>
      <Button variant="secondary" onClick={onCancel} width="100%">
        취소
      </Button>
      <Button variant="primary" onClick={onConfirm} width="100%">
        삭제
      </Button>
    </S.ConfirmButtons>
  </S.ConfirmBox>
);

const FriendItem = ({ friend }: { friend: Friend }) => {
  const { showToast } = useToast();
  const { openModal, closeModal } = useModal();
  const { removeFriendFromStore } = useFriends();
  const [loading, setLoading] = useState(false);

  const handleRemove = () => {
    openModal(
      <RemoveConfirm
        friend={friend}
        onConfirm={async () => {
          try {
            setLoading(true);
            await friendsApi.removeFriend(friend.userId);
            removeFriendFromStore(friend.userId);
            showToast({ message: '친구를 삭제했습니다', type: 'info' });
            closeModal();
          } catch (err) {
            const code = getErrorCode(err);
            if (code === 'NOT_FRIEND') {
              showToast({ message: '이미 친구 관계가 아닙니다', type: 'error' });
            } else {
              showToast({ message: '친구 삭제에 실패했습니다', type: 'error' });
            }
            setLoading(false);
          }
        }}
        onCancel={closeModal}
      />,
      { title: '친구 삭제', showCloseButton: true }
    );
  };

  return (
    <FriendRow
      nickname={friend.nickname}
      userCode={friend.userCode}
      online={friend.online}
      showOnlineDot
      right={
        <S.DeleteButton onClick={handleRemove} disabled={loading} aria-label="친구 삭제">
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
            <path d="M10 11v6M14 11v6" />
            <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
          </svg>
        </S.DeleteButton>
      }
    />
  );
};

const FriendsList = () => {
  const { friends } = useFriends();

  const sorted = [...friends].sort((a, b) => Number(b.online) - Number(a.online));

  if (sorted.length === 0) {
    return (
      <S.Empty>
        <svg
          width="48"
          height="48"
          viewBox="0 0 24 24"
          fill="none"
          stroke={theme.color.gray[300]}
          strokeWidth="1.5"
          strokeLinecap="round"
          aria-hidden="true"
        >
          <circle cx="9" cy="7" r="4" />
          <path d="M2 21v-2a4 4 0 0 1 4-4h6a4 4 0 0 1 4 4v2" />
          <path d="M19 8v6M22 11h-6" />
        </svg>
        <S.EmptyText>아직 친구가 없어요</S.EmptyText>
        <S.EmptyDesc>검색으로 친구를 추가해보세요</S.EmptyDesc>
      </S.Empty>
    );
  }

  return (
    <S.List>
      {sorted.map((friend) => (
        <S.Item key={friend.userId}>
          <FriendItem friend={friend} />
        </S.Item>
      ))}
    </S.List>
  );
};

export default FriendsList;

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
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 48px 16px;
    gap: 8px;
    text-align: center;
  `,

  EmptyText: styled.p`
    ${theme.typography.h4}
    color: ${theme.color.gray[700]};
  `,

  EmptyDesc: styled.p`
    ${theme.typography.small}
    color: ${theme.color.gray[400]};
  `,

  DeleteButton: styled.button`
    display: flex;
    align-items: center;
    justify-content: center;
    background: none;
    border: none;
    cursor: pointer;
    color: ${theme.color.gray[300]};
    padding: 6px;
    border-radius: 8px;
    transition:
      color 0.12s,
      background 0.12s;
    &:hover {
      color: ${theme.color.point[400]};
      background: ${theme.color.point[50]};
    }
    &:disabled {
      cursor: default;
      opacity: 0.4;
    }
  `,

  ConfirmBox: styled.div`
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 8px 0 4px;
  `,

  ConfirmText: styled.p`
    ${theme.typography.paragraph}
    color: ${theme.color.gray[700]};
    text-align: center;
  `,

  ConfirmButtons: styled.div`
    display: flex;
    gap: 8px;
  `,
};
