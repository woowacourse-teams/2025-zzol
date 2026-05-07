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
  padding: 10px 14px;
  background-color: ${({ theme }) => theme.color.gray[900]};
  color: ${({ theme }) => theme.color.white};
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  animation: ${slideInUp} 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
`;

export const Left = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
`;

export const Badge = styled.span<{ $on: boolean }>`
  ${({ theme }) => theme.typography.caption}
  font-weight: 700;
  letter-spacing: 0.04em;
  padding: 2px 7px;
  border-radius: 6px;
  background: ${({ $on, theme }) => ($on ? theme.color.point[400] : theme.color.gray[700])};
  color: ${({ theme }) => theme.color.white};
`;

export const Message = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[300]};
`;

export const ToggleButton = styled.button`
  padding: 5px 12px;
  border: none;
  border-radius: 8px;
  background: ${({ theme }) => theme.color.gray[700]};
  color: ${({ theme }) => theme.color.white};
  ${({ theme }) => theme.typography.small}
  font-weight: 700;
  cursor: pointer;
  flex-shrink: 0;

  &:active {
    opacity: 0.8;
  }
`;
