import { css } from '@emotion/react';
import styled from '@emotion/styled';
import Button from '@/components/@common/Button/Button';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { theme } from '@/styles/theme';

type Props = {
  inviterNickname: string;
  joinCode: string;
  onClose: () => void;
};

const RoomInvitationModal = ({ inviterNickname, joinCode, onClose }: Props) => {
  const navigate = useReplaceNavigate();
  const { joinCode: currentJoinCode } = useIdentifier();

  const handleJoin = () => {
    onClose();
    if (currentJoinCode) {
      // 이미 방에 있는 경우 — 현재 구현에서는 그냥 이동 (navigate to join)
      // 기존 방 입장 흐름 재사용 (닉네임 입력 → POST /rooms/{joinCode})
    }
    navigate(`/join/${joinCode}`);
  };

  return (
    <S.Container>
      <S.Text>
        <S.Nickname>{inviterNickname}</S.Nickname> 님이 방에 초대했습니다
      </S.Text>
      <S.JoinCode>방 코드: {joinCode}</S.JoinCode>
      <S.ButtonRow>
        <Button variant="secondary" onClick={onClose} width="100%">
          거절
        </Button>
        <Button variant="primary" onClick={handleJoin} width="100%">
          참여
        </Button>
      </S.ButtonRow>
    </S.Container>
  );
};

export default RoomInvitationModal;

const S = {
  Container: styled.div`
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 8px 0 4px;
  `,

  Text: styled.p`
    ${css(theme.typography.paragraph)}
    color: ${theme.color.gray[800]};
    text-align: center;
  `,

  Nickname: styled.span`
    font-weight: 700;
    color: ${theme.color.gray[900]};
  `,

  JoinCode: styled.p`
    ${css(theme.typography.small)}
    color: ${theme.color.gray[500]};
    text-align: center;
  `,

  ButtonRow: styled.div`
    display: flex;
    gap: 8px;
  `,
};
