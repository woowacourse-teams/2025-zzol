import { Z_INDEX } from '@/constants/zIndex';
import styled from '@emotion/styled';

type WrapperProps = {
  $isSpinStarted?: boolean;
  $finalRotation?: number;
};

export const Container = styled.div`
  width: 100%;
  height: 100%;
  position: relative;
`;

export const Wrapper = styled.div<WrapperProps>`
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: ${({ theme }) => theme.color.point[100]};
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  svg {
    width: 100%;
    height: 100%;
  }

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

// 휠 위로 뺀다. 안으로 파고들면 반경 방향으로 누운 12시 조각의 이름을 가린다.
export const Pin = styled.div`
  width: 0;
  height: 0;
  border-left: 11px solid transparent;
  border-right: 11px solid transparent;
  border-top: 26px solid ${({ theme }) => theme.color.gray[500]};
  border-radius: 4px;
  position: absolute;
  top: -24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: ${Z_INDEX.ROULETTE_PIN};
`;
