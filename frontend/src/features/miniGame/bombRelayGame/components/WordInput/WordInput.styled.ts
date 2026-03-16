import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

const borderPulse = keyframes`
  0%, 100% { border-color: #ff6b6b; }
  50% { border-color: #ff8e53; }
`;

export const Container = styled.div`
  display: flex;
  gap: 8px;
  width: 100%;
`;

export const Input = styled.input<{ $isMyTurn: boolean }>`
  flex: 1;
  padding: 12px 16px;
  font-size: 1.1rem;
  border: 2px solid #ddd;
  border-radius: 12px;
  outline: none;
  transition: all 0.3s;

  ${({ $isMyTurn }) =>
    $isMyTurn &&
    css`
      border-color: #ff6b6b;
      animation: ${borderPulse} 1.5s ease-in-out infinite;
      background-color: #fff8f8;
    `}

  &:focus {
    border-color: #ff6b6b;
    box-shadow: 0 0 0 3px rgba(255, 107, 107, 0.15);
  }

  &:disabled {
    background-color: #f5f5f5;
    color: #aaa;
    animation: none;
  }
`;

export const SubmitButton = styled.button<{ $disabled: boolean }>`
  padding: 12px 20px;
  font-size: 1rem;
  font-weight: 700;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s;

  ${({ $disabled }) =>
    $disabled
      ? css`
          background-color: #ddd;
          color: #999;
          cursor: not-allowed;
        `
      : css`
          background-color: #ff6b6b;
          color: white;

          &:active {
            background-color: #e55a5a;
          }
        `}
`;
