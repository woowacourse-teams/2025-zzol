import { buttonHoverPress } from '@/styles/animations/buttonHoverPress';

import { Size } from '@/types/styles';
import { TouchState } from '@/types/touchState';
import { css, keyframes } from '@emotion/react';
import styled from '@emotion/styled';

export type ButtonVariant = 'primary' | 'secondary' | 'disabled' | 'loading' | 'ready';

type Props = {
  $variant: ButtonVariant;
  $width: string;
  $height: Size;
  $touchState: TouchState;
  $isLoading: boolean;
};

export const Container = styled.button<Props>`
  width: ${({ $width }) => $width};
  height: ${({ $height }) => {
    switch ($height) {
      case 'small':
        return '40px';
      case 'medium':
        return '45px';
      case 'large':
        return '50px';
      default:
        return '50px';
    }
  }};
  ${({ theme }) => theme.typography.h4}
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 12px;
  cursor: pointer;

  ${({ $variant, theme, $touchState }) => {
    switch ($variant) {
      case 'secondary':
        return css`
          background: ${theme.color.gray[50]};
          color: ${theme.color.gray[700]};
          ${buttonHoverPress({ activeColor: theme.color.gray[100], touchState: $touchState })}
        `;
      case 'disabled':
        return css`
          background: ${theme.color.gray[200]};
          color: ${theme.color.white};
          cursor: default;
          opacity: 0.7;
        `;
      case 'ready':
        return css`
          background: ${theme.color.point[50]};
          color: ${theme.color.point[400]};
        `;
      default:
        return css`
          background: ${theme.color.point[400]};
          color: ${theme.color.white};
          ${buttonHoverPress({ activeColor: theme.color.point[500], touchState: $touchState })}
        `;
    }
  }}

  ${({ $variant, $isLoading, theme }) =>
    ($variant === 'loading' || $isLoading) &&
    css`
      cursor: default;
      background: ${theme.color.point[300]};
      color: transparent;
      pointer-events: none;

      &:hover,
      &:active,
      &:focus {
        transform: none;
        box-shadow: none;
        outline: none;
      }
    `}
`;

const dotPulse = keyframes`
  0%, 20% {
    opacity: 0.3;
  }
  50% {
    opacity: 1;
  }
  80%, 100% {
    opacity: 0.3;
  }
`;

export const LoadingDots = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;

  span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: white;
    animation: ${dotPulse} 1.2s infinite ease-in-out;

    &:nth-of-type(1) {
      animation-delay: 0s;
    }
    &:nth-of-type(2) {
      animation-delay: 0.2s;
    }
    &:nth-of-type(3) {
      animation-delay: 0.4s;
    }
  }
`;

const textPulse = keyframes`
  0%, 20% {
    opacity: 0.3;
  }
  50% {
    opacity: 1;
  }
  80%, 100% {
    opacity: 0.3;
  }
`;

export const LoadingText = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;

  span {
    animation: ${textPulse} 1.2s infinite ease-in-out;
  }

  span:nth-of-type(1) {
    animation-delay: 0s;
  }
  span:nth-of-type(2) {
    animation-delay: 0.1s;
  }
  span:nth-of-type(3) {
    animation-delay: 0.2s;
  }
  span:nth-of-type(4) {
    animation-delay: 0.3s;
  }
  span:nth-of-type(5) {
    animation-delay: 0.4s;
  }
  span:nth-of-type(6) {
    animation-delay: 0.5s;
  }
  span:nth-of-type(7) {
    animation-delay: 0.6s;
  }
  span:nth-of-type(8) {
    animation-delay: 0.7s;
  }
  span:nth-of-type(9) {
    animation-delay: 0.8s;
  }
  span:nth-of-type(10) {
    animation-delay: 0.9s;
  }
`;
