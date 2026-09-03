import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

type Props = {
  $direction: 'ahead' | 'behind';
};

export const Badge = styled.div<Props>`
  position: absolute;
  right: 14px;
  /* 아래쪽 배지는 바닥 눈금 위에 올린다. */
  ${({ $direction }) =>
    $direction === 'ahead' ? 'top: 8px;' : 'bottom: calc(52px + env(safe-area-inset-bottom));'}
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border: 1px solid ${({ theme }) => theme.color.white}38;
  border-radius: 12px;
  background: ${({ theme }) => theme.color.gray[900]}9E;
  z-index: ${RACING_Z_INDEX.PLAYER};
`;

export const Arrow = styled.span<Props>`
  width: 0;
  height: 0;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  ${({ $direction, theme }) =>
    $direction === 'ahead'
      ? `border-bottom: 6px solid ${theme.color.white};`
      : `border-top: 6px solid ${theme.color.white};`}
`;

export const Label = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.white};
`;
