import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';

const slideInFromRight = keyframes`
  from {
    transform: translateX(30%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
`;

const slideOutToLeft = keyframes`
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(-30%);
    opacity: 0;
  }
`;

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;

  &.slide-first {
    animation:
      ${slideInFromRight} 0.4s ease-out 0s forwards,
      ${slideOutToLeft} 0.4s ease-in 2s forwards;
  }

  &.slide-second {
    opacity: 0;
    transform: translateX(30%);
    animation: ${slideInFromRight} 0.4s ease-out 2s forwards;
  }
`;

export const TextWrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 2rem;
`;

export const ImageWrapper = styled.div`
  width: 50%;
  max-width: 250px;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
`;

export const Image = styled.img`
  width: 100%;
  height: 100%;
  object-fit: contain;
`;
