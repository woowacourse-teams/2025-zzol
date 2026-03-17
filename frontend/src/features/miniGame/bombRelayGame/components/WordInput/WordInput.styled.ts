import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

const borderPulse = keyframes`
  0%, 100% { border-color: #ff6b6b; }
  50% { border-color: #ff8e53; }
`;

export const Container = styled.div`
  display: flex;
  gap: 10px;
  width: 100%;
`;

export const Input = styled.input<{ $isMyTurn: boolean }>`
  flex: 1;
  padding: 14px 18px;
  font-size: 1rem;
  font-weight: 500;
  border: 2px solid #e8e8e8;
  border-radius: 16px;
  outline: none;
  background: white;
  transition: all 0.3s;

  ${({ $isMyTurn }) =>
    $isMyTurn &&
    css`
      border-color: #ff6b6b;
      animation: ${borderPulse} 1.5s ease-in-out infinite;
      background-color: #fffafa;
    `}

  &:focus {
    border-color: #ff6b6b;
    box-shadow: 0 0 0 3px rgba(255, 107, 107, 0.1);
  }

  &:disabled {
    background-color: #f8f8f8;
    color: #bbb;
    border-color: #eee;
    animation: none;
  }
`;

export const SubmitButton = styled.button<{ $disabled: boolean }>`
  padding: 14px 24px;
  font-size: 0.95rem;
  font-weight: 700;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  ${({ $disabled }) =>
    $disabled
      ? css`
          background-color: #e8e8e8;
          color: #bbb;
          cursor: not-allowed;
        `
      : css`
          background: linear-gradient(135deg, #ff6b6b, #ff8e53);
          color: white;
          box-shadow: 0 3px 10px rgba(255, 107, 107, 0.3);

          &:active {
            transform: scale(0.97);
          }
        `}
`;
