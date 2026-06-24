import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';

type Props = {
  $width: string | number;
  $height: string | number;
  $borderRadius: string | number;
};

const shimmer = keyframes`
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
`;

const formatValue = (value: string | number) => {
  return typeof value === 'number' ? `${value}px` : value;
};

export const Container = styled.div<Props>`
  width: ${({ $width }) => formatValue($width)};
  height: ${({ $height }) => formatValue($height)};
  border-radius: ${({ $borderRadius }) => formatValue($borderRadius)};
  background: linear-gradient(
    90deg,
    ${({ theme }) => theme.color.gray[200]} 0%,
    ${({ theme }) => theme.color.gray[100]} 50%,
    ${({ theme }) => theme.color.gray[200]} 100%
  );
  background-size: 200% 100%;
  animation: ${shimmer} 1.5s ease-in-out infinite;
`;
