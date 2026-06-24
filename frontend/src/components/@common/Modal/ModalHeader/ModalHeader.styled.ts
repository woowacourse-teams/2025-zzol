import { rippleEffect } from '@/styles/animations/effects/rippleEffect';
import { TouchState } from '@/types/touchState';
import styled from '@emotion/styled';

type CloseButtonProps = {
  $touchState: TouchState;
};

export const Container = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  width: 100%;
  min-height: 32px;
  padding-bottom: 16px;
`;

export const CloseButton = styled.button<CloseButtonProps>`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: none;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  position: absolute;
  right: 0;
  top: 0;

  ${({ $touchState }) => rippleEffect({ touchState: $touchState })}
`;
