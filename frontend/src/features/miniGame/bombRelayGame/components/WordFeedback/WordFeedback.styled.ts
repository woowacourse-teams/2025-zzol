import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

const slideIn = keyframes`
  0% { opacity: 0; transform: translateY(8px); }
  100% { opacity: 1; transform: translateY(0); }
`;

export const Container = styled.div<{ $accepted: boolean }>`
  text-align: center;
  padding: 8px 16px;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 600;
  animation: ${slideIn} 0.3s ease forwards;
  min-height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;

  ${({ $accepted }) =>
    $accepted
      ? css`
          background-color: #f0faf0;
          color: #2e7d32;
        `
      : css`
          background-color: #fff5f5;
          color: #d32f2f;
        `}
`;
