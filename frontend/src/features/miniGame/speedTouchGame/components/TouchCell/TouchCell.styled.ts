import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

const flipOut = keyframes`
  0% { transform: rotateY(0deg); }
  50% { transform: rotateY(90deg); }
  100% { transform: rotateY(0deg); }
`;

type CellProps = {
  $touched: boolean;
};

export const Cell = styled.button<CellProps>`
  aspect-ratio: 1;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  -webkit-tap-highlight-color: transparent;
  user-select: none;

  ${({ $touched, theme }) =>
    $touched
      ? css`
          background-color: ${theme.color.point[400]};
          border: 2px solid ${theme.color.point[200]};
          cursor: default;
          animation: ${flipOut} 0.3s ease;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
        `
      : css`
          background-color: ${theme.color.white};
          border: 2px solid ${theme.color.gray[200]};
          cursor: pointer;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
          transition:
            transform 0.15s ease,
            box-shadow 0.15s ease;

          &:active {
            transform: scale(0.93);
            box-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
          }
        `}
`;

export const CoffeeCircle = styled.div`
  width: 60%;
  height: 60%;
  border-radius: 50%;
  background-color: ${({ theme }) => theme.color.point[300]};
  display: flex;
  align-items: center;
  justify-content: center;
`;

export const CoffeeIcon = styled.img`
  width: 70%;
  height: 70%;
`;

export const Number = styled.span`
  font-size: 1.2rem;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[700]};
`;
