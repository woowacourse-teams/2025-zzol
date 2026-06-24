import { DESIGN_TOKENS } from '@/constants/design';
import { rippleEffect } from '@/styles/animations/effects/rippleEffect';
import { TouchState } from '@/types/touchState';
import styled from '@emotion/styled';

type ContainerProps = { $height: string; $hasValue: boolean };
type ClearButtonProps = { $hasValue: boolean; $touchState: TouchState };

export const Container = styled.div<ContainerProps>`
  display: flex;
  align-items: center;
  width: 100%;
  height: ${({ $height }) => $height};
  border-bottom: ${({ theme, $hasValue }) =>
    `2px solid ${$hasValue ? theme.color.gray[400] : theme.color.gray[200]}`};
`;

export const Input = styled.input`
  flex: 1;
  outline: none;
  border: none;
  background: transparent;
  padding: 4px;

  width: 80%;
  color: ${({ theme }) => theme.color.gray[700]};
  ${({ theme }) => theme.typography.h4};

  &:hover:not(:disabled) {
    border-color: #666666;
  }

  &:disabled {
    cursor: default;
  }

  &::placeholder {
    color: ${({ theme }) => theme.color.gray[300]};
    opacity: 1;
  }

  &:-webkit-autofill {
    -webkit-box-shadow: 0 0 0 1000px white inset;
    -webkit-text-fill-color: ${({ theme }) => theme.color.gray[900]};
  }
`;

export const ClearButton = styled.button<ClearButtonProps>`
  background-color: white;
  font-size: ${DESIGN_TOKENS.typography.h2};
  display: flex;
  justify-content: center;
  align-items: center;
  border: none;
  outline: none;
  cursor: pointer;
  visibility: ${({ $hasValue }) => ($hasValue ? 'visible' : 'hidden')};

  position: relative;
  ${({ $touchState }) => rippleEffect({ touchState: $touchState, diameter: '20px' })}
`;
