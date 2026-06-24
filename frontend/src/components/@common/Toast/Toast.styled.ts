import { Z_INDEX } from '@/constants/zIndex';
import { keyframes } from '@emotion/react';
import styled from '@emotion/styled';
import { ToastType } from './types';

type Props = {
  $type: ToastType;
};

const slideInUp = keyframes`
  from {
    transform: translateX(-50%) translateY(100%);
    opacity: 0;
  }
  to {
    transform: translateX(-50%) translateY(0);
    opacity: 1;
  }
`;

const slideOutDown = keyframes`
  from {
    transform: translateX(-50%) translateY(0);
    opacity: 1;
  }
  to {
    transform: translateX(-50%) translateY(100%);
    opacity: 0;
  }
`;

const TOAST_COLORS: Record<ToastType, string> = {
  success: '#f0fdf4',
  error: '#fef2f2',
  warning: '#fffbeb',
  info: '#eff6ff',
} as const;

export const Container = styled.div<Props>`
  position: fixed;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 90%;
  height: 50px;
  max-width: 400px;
  background: ${({ $type }) => TOAST_COLORS[$type]};
  border-radius: 16px;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.12),
    0 1px 2px rgba(0, 0, 0, 0.24);
  padding: 4px 16px;
  z-index: ${Z_INDEX.TOAST};
  bottom: 82px;
  left: 50%;
  transform: translateX(-50%);

  animation: ${slideInUp} 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
  &.toast-exit {
    animation: ${slideOutDown} 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55) forwards;
    pointer-events: none;
  }

  > * {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
  }
`;

export const IconWrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
`;
