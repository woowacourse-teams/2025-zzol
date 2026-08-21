import styled from '@emotion/styled';
import { useState } from 'react';
import { ApiError } from '@/apis/rest/error';
import Button from '@/components/@common/Button/Button';
import useToast from '@/components/@common/Toast/useToast';
import { friendsApi } from '@/features/friends/api/friendsApi';
import { useFriends } from '@/features/friends/hooks/useFriends';
import { Friend } from '@/features/friends/types';
import { theme } from '@/styles/theme';

const getErrorCode = (err: unknown): string | undefined =>
  err instanceof ApiError ? (err.data as { errorCode?: string } | null)?.errorCode : undefined;

type Props = {
  joinCode: string;
  onClose: () => void;
};

/** 초대장은 저장되지 않고 개인 큐로만 전달되므로, 받을 수 없는 상태면 보내지 않는다(서버도 같은 조건으로 거절한다).
 *  다른 방에 있는 친구는 막지 않는다 — 방을 옮기라고 부르는 것이 초대의 쓰임 중 하나다. */
const inviteBlockReason = (friend: Friend, joinCode: string): string | null => {
  if (friend.joinCode === joinCode) return '참가 중';
  if (!friend.online) return '오프라인';
  return null;
};

const FriendInviteRow = ({ friend, joinCode }: { friend: Friend; joinCode: string }) => {
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
      } else if (code === 'ROOM_NOT_READY_TO_JOIN') {
        showToast({ message: '게임이 시작된 방에는 초대할 수 없습니다', type: 'error' });
      } else if (code === 'NOT_FRIEND') {
        showToast({ message: '친구 관계가 아닙니다', type: 'error' });
      } else if (code === 'ROOM_NOT_FOUND') {
        showToast({ message: '존재하지 않는 방입니다', type: 'error' });
      } else if (code === 'FRIEND_OFFLINE') {
        showToast({ message: '접속 중이 아닌 친구는 초대할 수 없습니다', type: 'error' });
      } else {
        showToast({ message: '초대에 실패했습니다', type: 'error' });
      }
    } finally {
      setLoading(false);
    }
  };

  const blockReason = inviteBlockReason(friend, joinCode);

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
      {blockReason ? (
        <Button variant="disabled" width="88px" height="small">
          {blockReason}
        </Button>
      ) : invited ? (
        <Button variant="disabled" width="72px" height="small">
          초대완료
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

const InviteFriendModal = ({ joinCode, onClose }: Props) => {
  // 컨텍스트에서 직접 읽는다 — openModal이 엘리먼트를 보관하므로 prop으로 받으면 연 시점 값이 클로저에
  // 고정돼, 모달이 열려 있는 동안 친구가 방에 들어오거나 나가도 버튼 상태가 갱신되지 않는다.
  const { friends } = useFriends();

  // 초대 가능한 친구를 맨 위로
  const sorted = [...friends].sort(
    (a, b) =>
      Number(inviteBlockReason(a, joinCode) !== null) -
        Number(inviteBlockReason(b, joinCode) !== null) || Number(b.online) - Number(a.online)
  );

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
            <FriendInviteRow friend={friend} joinCode={joinCode} />
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
