import styled from '@emotion/styled';
import { useState } from 'react';
import { ApiError } from '@/apis/rest/error';
import Button from '@/components/@common/Button/Button';
import useToast from '@/components/@common/Toast/useToast';
import { friendsApi } from '@/features/friends/api/friendsApi';
import { Friend } from '@/features/friends/types';
import { theme } from '@/styles/theme';

const getErrorCode = (err: unknown): string | undefined =>
  err instanceof ApiError ? (err.data as { errorCode?: string } | null)?.errorCode : undefined;

type Props = {
  joinCode: string;
  friends: Friend[];
  participantNames: Set<string>;
  onClose: () => void;
};

const FriendInviteRow = ({
  friend,
  joinCode,
  isParticipant,
}: {
  friend: Friend;
  joinCode: string;
  isParticipant: boolean;
}) => {
  const { showToast } = useToast();
  const [invited, setInvited] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleInvite = async () => {
    try {
      setLoading(true);
      await friendsApi.inviteToRoom(joinCode, friend.userId);
      setInvited(true);
      showToast({ message: `${friend.nickname} 님에게 초대장을 보냈습니다`, type: 'success' });
      setTimeout(() => setInvited(false), 3000);
    } catch (err: unknown) {
      const code = getErrorCode(err);
      if (code === 'ROOM_FULL') {
        showToast({ message: '방이 가득 찼습니다', type: 'error' });
      } else if (code === 'ROOM_INVITATION_NOT_LOBBY') {
        showToast({ message: '게임이 시작된 방에는 초대할 수 없습니다', type: 'error' });
      } else if (code === 'NOT_FRIEND') {
        showToast({ message: '친구 관계가 아닙니다', type: 'error' });
      } else if (code === 'ROOM_NOT_FOUND') {
        showToast({ message: '존재하지 않는 방입니다', type: 'error' });
      } else {
        showToast({ message: '초대에 실패했습니다', type: 'error' });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <S.Row>
      <S.AvatarWrap>
        <S.Avatar>{friend.nickname?.slice(0, 1) ?? '?'}</S.Avatar>
        <S.OnlineDot $online={friend.online} />
      </S.AvatarWrap>
      <S.Info>
        <S.Nickname>{friend.nickname}</S.Nickname>
        <S.Code># {friend.userCode}</S.Code>
      </S.Info>
      {isParticipant ? (
        <Button variant="disabled" width="72px" height="small">
          참가 중
        </Button>
      ) : invited ? (
        <Button variant="disabled" width="72px" height="small">
          초대됨
        </Button>
      ) : (
        <Button
          variant="primary"
          onClick={handleInvite}
          isLoading={loading}
          width="72px"
          height="small"
        >
          초대
        </Button>
      )}
    </S.Row>
  );
};

const InviteFriendModal = ({ joinCode, friends, participantNames, onClose }: Props) => {
  const sorted = [...friends].sort((a, b) => Number(b.online) - Number(a.online));

  if (sorted.length === 0) {
    return (
      <S.Empty>
        <S.EmptyText>아직 친구가 없어요</S.EmptyText>
        <S.EmptyDesc>홈 화면 친구 탭에서 친구를 추가해보세요</S.EmptyDesc>
        <Button variant="secondary" onClick={onClose} width="120px" height="small">
          닫기
        </Button>
      </S.Empty>
    );
  }

  return (
    <S.Container>
      <S.List>
        {sorted.map((friend) => (
          <S.Item key={friend.userId}>
            <FriendInviteRow
              friend={friend}
              joinCode={joinCode}
              isParticipant={participantNames.has(friend.nickname)}
            />
          </S.Item>
        ))}
      </S.List>
    </S.Container>
  );
};

export default InviteFriendModal;

const S = {
  Container: styled.div`
    max-height: 360px;
    overflow-y: auto;
  `,

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

  Row: styled.div`
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
  `,

  AvatarWrap: styled.div`
    position: relative;
    flex-shrink: 0;
  `,

  Avatar: styled.div`
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: ${theme.color.point[100]};
    color: ${theme.color.point[500]};
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 14px;
  `,

  OnlineDot: styled.div<{ $online: boolean }>`
    position: absolute;
    bottom: 0;
    right: 0;
    width: 9px;
    height: 9px;
    border-radius: 50%;
    border: 2px solid ${theme.color.white};
    background: ${({ $online }) =>
      $online ? theme.color.status.online : theme.color.status.offline};
  `,

  Info: styled.div`
    flex: 1;
    min-width: 0;
  `,

  Nickname: styled.p`
    font-size: 14px;
    font-weight: 600;
    color: ${theme.color.gray[900]};
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  `,

  Code: styled.p`
    font-size: 11px;
    color: ${theme.color.gray[400]};
    margin-top: 1px;
  `,

  Empty: styled.div`
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 32px 16px 16px;
    text-align: center;
  `,

  EmptyText: styled.p`
    ${theme.typography.h4}
    color: ${theme.color.gray[700]};
  `,

  EmptyDesc: styled.p`
    ${theme.typography.small}
    color: ${theme.color.gray[400]};
    margin-bottom: 8px;
  `,
};
