import { keyframes } from '@emotion/react';
import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

export const Overlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  z-index: ${RACING_Z_INDEX.OVERLAY};
  /* 화면 전체가 탭 영역이라 더블탭 확대와 정면으로 부딪친다. 눈치게임 키패드와 같은 방어다. */
  touch-action: manipulation;
`;

const rippleExpand = keyframes`
  from {
    transform: translate(-50%, -50%) scale(0.35);
    opacity: 0.6;
  }
  to {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0;
  }
`;

export const Ripple = styled.span`
  position: absolute;
  width: 74px;
  height: 74px;
  border-radius: 50%;
  border: 2px solid ${({ theme }) => theme.color.white}8C;
  background: ${({ theme }) => theme.color.white}38;
  pointer-events: none;
  animation: ${rippleExpand} 420ms ease-out forwards;
  z-index: ${RACING_Z_INDEX.BANNER};
`;

export const DisconnectBanner = styled.div`
  position: absolute;
  left: 50%;
  bottom: calc(1.5rem + env(safe-area-inset-bottom));
  transform: translateX(-50%);
  width: 90%;
  max-width: 400px;
  padding: 10px 14px;
  border-radius: 10px;
  text-align: center;
  background: ${({ theme }) => theme.color.gray[900]}E0;
  z-index: ${RACING_Z_INDEX.BANNER};
`;

export const DisconnectTitle = styled.p`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.white};
`;

export const DisconnectHint = styled.p`
  ${({ theme }) => theme.typography.caption}
  margin-top: 2px;
  color: ${({ theme }) => theme.color.gray[300]};
`;
