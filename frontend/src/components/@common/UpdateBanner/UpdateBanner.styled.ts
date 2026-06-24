import { keyframes } from '@emotion/react';
import styled from '@emotion/styled';
import { Z_INDEX } from '@/constants/zIndex';

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

export const Banner = styled.div`
  position: fixed;
  bottom: 82px;
  left: 50%;
  transform: translateX(-50%);
  width: 90%;
  max-width: 400px;
  z-index: ${Z_INDEX.TOAST};
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 16px;
  background-color: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  border-radius: 16px;
  box-shadow: 0 4px 20px ${({ theme }) => theme.color.point[400]}59;
  animation: ${slideInUp} 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
`;

export const Message = styled.span`
  flex: 1;
  font-size: ${({ theme }) => theme.typography.paragraph.fontSize};
  font-weight: ${({ theme }) => theme.typography.paragraph.fontWeight};
  line-height: ${({ theme }) => theme.typography.paragraph.lineHeight};
`;

export const Actions = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
`;

export const UpdateButton = styled.button`
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  background: ${({ theme }) => theme.color.white};
  color: ${({ theme }) => theme.color.point[500]};
  font-size: ${({ theme }) => theme.typography.small.fontSize};
  font-weight: 700;
  cursor: pointer;

  &:active {
    opacity: 0.85;
  }
`;

export const CloseButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: ${({ theme }) => theme.color.point[100]};
  font-size: 16px;
  line-height: 1;
  cursor: pointer;

  &:active {
    opacity: 0.7;
  }
`;
