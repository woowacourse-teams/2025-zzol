import styled from '@emotion/styled';
import { ReactNode } from 'react';
import { theme } from '@/styles/theme';

type Props = {
  nickname?: string;
  userCode?: string;
  online?: boolean;
  showOnlineDot?: boolean;
  right?: ReactNode;
};

const FriendRow = ({ nickname, userCode, online = false, showOnlineDot = false, right }: Props) => (
  <S.Row>
    <S.AvatarWrap>
      <S.Avatar>{nickname?.slice(0, 1) ?? '?'}</S.Avatar>
      {showOnlineDot && <S.OnlineDot $online={online} />}
    </S.AvatarWrap>
    <S.Info>
      <S.Nickname>{nickname}</S.Nickname>
      <S.Code># {userCode}</S.Code>
    </S.Info>
    {right && <S.Right>{right}</S.Right>}
  </S.Row>
);

export default FriendRow;

const S = {
  Row: styled.div`
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    background: ${theme.color.white};
  `,

  AvatarWrap: styled.div`
    position: relative;
    flex-shrink: 0;
  `,

  Avatar: styled.div`
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: linear-gradient(
      135deg,
      ${theme.color.point[200]} 0%,
      ${theme.color.point[400]} 100%
    );
    color: ${theme.color.white};
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 800;
    font-size: 16px;
    letter-spacing: -0.02em;
    box-shadow: 0 2px 6px rgba(253, 108, 110, 0.3);
  `,

  OnlineDot: styled.div<{ $online: boolean }>`
    position: absolute;
    bottom: 1px;
    right: 1px;
    width: 10px;
    height: 10px;
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
    ${theme.typography.h4}
    color: ${theme.color.gray[900]};
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    line-height: 1.2;
  `,

  Code: styled.p`
    ${theme.typography.caption}
    color: ${theme.color.gray[400]};
    margin-top: 0;
    line-height: 1.2;
  `,

  Right: styled.div`
    display: flex;
    gap: 6px;
    align-items: center;
    flex-shrink: 0;
  `,
};
