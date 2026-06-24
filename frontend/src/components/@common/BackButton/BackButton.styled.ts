import { buttonHoverPress } from '@/styles/animations/buttonHoverPress';
import { rippleEffect } from '@/styles/animations/effects/rippleEffect';
import { TouchState } from '@/types/touchState';
import styled from '@emotion/styled';

type Props = {
  $touchState: TouchState;
  $hasText: boolean;
};

export const Container = styled.button<Props>`
  position: relative;
  cursor: pointer;
  min-width: 16px;
  height: ${({ $hasText }) => ($hasText ? '36px' : '16px')};
  width: ${({ $hasText }) => $hasText && 'auto'};
  padding: ${({ $hasText }) => $hasText && '0 7px 0 3px'};
  display: flex;
  align-items: center;
  justify-content: start;
  gap: 4px;

  ${({ $touchState, $hasText }) => !$hasText && rippleEffect({ touchState: $touchState })}

  ${({ theme, $touchState, $hasText }) =>
    $hasText &&
    buttonHoverPress({
      activeColor: theme.color.gray[100],
      touchState: $touchState,
      enableScale: true,
      scaleValue: 0.98,
    })}
`;

export const Text = styled.span`
  color: #c0c0c0;
  font-weight: 700;
  font-size: 14px;
  line-height: 16px;
  height: 16px;
`;
