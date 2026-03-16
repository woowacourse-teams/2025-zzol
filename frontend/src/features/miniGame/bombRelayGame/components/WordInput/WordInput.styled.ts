import styled from '@emotion/styled';
import { css } from '@emotion/react';

export const Container = styled.div`
  display: flex;
  gap: 8px;
  width: 100%;
`;

export const Input = styled.input`
  flex: 1;
  padding: 12px 16px;
  font-size: 1.1rem;
  border: 2px solid #ddd;
  border-radius: 12px;
  outline: none;
  transition: border-color 0.2s;

  &:focus {
    border-color: #ff6b6b;
  }

  &:disabled {
    background-color: #f5f5f5;
    color: #aaa;
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
