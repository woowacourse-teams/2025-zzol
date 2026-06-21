import styled from '@emotion/styled';
import { css } from '@emotion/react';
import { NunchiLocalInputState } from '@/types/miniGame/nunchiGame';

export const Keypad = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  width: 100%;
`;

export const Warning = styled.div`
  width: 100%;
  padding: 8px 12px;
  border-radius: 8px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.white};
  background: ${({ theme }) => theme.color.red};
`;

export const PressButton = styled.button<{
  $state: NunchiLocalInputState;
  $connected: boolean;
}>`
  width: 100%;
  /* 큰 타겟(요구사항 5) — 하단 절반을 채운다. */
  min-height: 180px;
  border: none;
  border-radius: 20px;
  font-size: 28px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.white};
  cursor: pointer;

  /* 더블탭 줌·텍스트 선택·길게누르기 방지(요구사항 5/J). */
  touch-action: manipulation;
  user-select: none;
  -webkit-user-select: none;
  -webkit-tap-highlight-color: transparent;

  transition:
    transform 0.08s ease,
    background 0.2s ease;

  &:active:not(:disabled) {
    transform: scale(0.97);
  }

  ${({ $state, $connected, theme }) => {
    if (!$connected) {
      return css`
        background: ${theme.color.gray[400]};
      `;
    }
    switch ($state) {
      case 'STOOD':
        return css`
          background: ${theme.color.point[500]};
        `;
      case 'COLLIDED':
        return css`
          background: ${theme.color.gray[500]};
        `;
      case 'PRESSED':
        return css`
          background: ${theme.color.point[300]};
        `;
      case 'IDLE':
      default:
        return css`
          background: ${theme.color.point[400]};
        `;
    }
  }}

  &:disabled {
    cursor: not-allowed;
    opacity: 0.85;
  }
`;
