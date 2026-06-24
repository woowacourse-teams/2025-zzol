import styled from '@emotion/styled';

type Props = {
  $index: number;
  $delay: number;
  $duration: number;
};

export const Wrapper = styled.div<Props>`
  animation: fadeInUp ${({ $duration }) => $duration}ms ease-out
    ${({ $index, $delay }) => $index * $delay}ms both;

  @keyframes fadeInUp {
    0% {
      opacity: 0;
      transform: translateY(20px);
    }
    100% {
      opacity: 1;
      transform: translateY(0);
    }
  }
`;
