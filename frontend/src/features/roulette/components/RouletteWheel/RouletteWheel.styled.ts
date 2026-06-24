import { Z_INDEX } from '@/constants/zIndex';
import styled from '@emotion/styled';

type WrapperProps = {
  $isSpinStarted?: boolean;
  $finalRotation?: number;
};

export const Container = styled.div`
  width: 300px;
  height: 300px;
  position: relative;
`;

export const Wrapper = styled.div<WrapperProps>`
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background-color: ${({ theme }) => theme.color.point[100]};
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  --final-rotation: ${({ $finalRotation }) => $finalRotation ?? 0}deg;

  ${({ $isSpinStarted }) =>
    $isSpinStarted &&
    `
      animation: spin 3s cubic-bezier(0.33, 1, 0.68, 1);
      animation-fill-mode: forwards;
    `}

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(calc(1080deg + var(--final-rotation)));
    }
  }
`;

export const Pin = styled.div`
  width: 0;
  height: 0;
  border-left: 12px solid transparent;
  border-right: 12px solid transparent;
  border-top: 30px solid ${({ theme }) => theme.color.gray[500]};
  border-radius: 4px;
  position: absolute;
  top: -5px;
  left: 50%;
  transform: translateX(-50%);
  z-index: ${Z_INDEX.ROULETTE_PIN};
`;
