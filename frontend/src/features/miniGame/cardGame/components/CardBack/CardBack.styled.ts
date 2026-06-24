import { Size } from '@/types/styles';
import { css } from '@emotion/react';
import styled from '@emotion/styled';
import { cardVariants, circleVariants } from '../../constants/variants';

type Props = {
  $size?: Size;
  $disabled?: boolean;
};

export const Container = styled.button<Props>`
  ${({ $size }) => cardVariants[$size || 'large']}
  border: 2px solid ${({ theme }) => theme.color.point[200]};
  background-color: ${({ theme }) => theme.color.point[400]};

  border-radius: 7px;
  box-shadow: 0 3px 3px rgba(0, 0, 0, 0.4);
  position: relative;
  cursor: default;

  ${({ $disabled }) =>
    !$disabled &&
    css`
      cursor: pointer;
      transition:
        transform 0.2s ease,
        box-shadow 0.2s ease;

      &:active {
        transform: scale(0.98);
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
      }
    `}
`;

export const Circle = styled.div<Props>`
  background-color: ${({ theme }) => theme.color.point[300]};
  width: ${({ $size }) => circleVariants[$size || 'large']};
  height: ${({ $size }) => circleVariants[$size || 'large']};

  border-radius: 50%;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
`;

export const CoffeeIcon = styled.img`
  width: 100%;
  height: 100%;
`;
