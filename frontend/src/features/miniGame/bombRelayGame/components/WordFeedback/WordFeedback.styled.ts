import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

const fadeInOut = keyframes`
  0% { opacity: 0; transform: translateY(10px); }
  15% { opacity: 1; transform: translateY(0); }
  85% { opacity: 1; transform: translateY(0); }
  100% { opacity: 0; transform: translateY(-10px); }
`;

export const Container = styled.div<{ $accepted: boolean }>`
  text-align: center;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  animation: ${fadeInOut} 2s ease forwards;
  min-height: 36px;

  ${({ $accepted }) =>
    $accepted
      ? css`
          background-color: #e8f5e9;
          color: #2e7d32;
        `
      : css`
          background-color: #ffebee;
          color: #c62828;
        `}
`;
