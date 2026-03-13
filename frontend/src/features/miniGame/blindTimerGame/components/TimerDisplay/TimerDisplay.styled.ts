import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

const pulse = keyframes`
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
`;

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
`;

export const TimeText = styled.div<{ $isBlind: boolean }>`
  font-size: 4rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  letter-spacing: 2px;

  ${({ $isBlind, theme }) =>
    $isBlind
      ? css`
          color: ${theme.color.point[400]};
          animation: ${pulse} 1.5s ease-in-out infinite;
        `
      : css`
          color: ${theme.color.gray[800]};
        `}
`;

export const StopButton = styled.button<{ $disabled: boolean }>`
  width: 140px;
  height: 140px;
  border-radius: 50%;
  border: 4px solid ${({ theme }) => theme.color.point[300]};
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: 2px;
  -webkit-tap-highlight-color: transparent;
  user-select: none;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;

  ${({ $disabled, theme }) =>
    $disabled
      ? css`
          background-color: ${theme.color.gray[200]};
          color: ${theme.color.gray[400]};
          border-color: ${theme.color.gray[300]};
          cursor: default;
        `
      : css`
          background-color: ${theme.color.point[500]};
          color: ${theme.color.white};
          cursor: pointer;
          box-shadow: 0 4px 12px rgba(245, 62, 65, 0.4);

          &:active {
            transform: scale(0.95);
            box-shadow: 0 2px 6px rgba(245, 62, 65, 0.4);
          }
        `}
`;

export const StoppedResult = styled.div`
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const StoppedLabel = styled.span`
  font-size: 0.9rem;
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[500]};
`;

export const StoppedTime = styled.span`
  font-size: 2rem;
  font-weight: 700;
  color: ${({ theme }) => theme.color.point[500]};
  font-variant-numeric: tabular-nums;
`;
