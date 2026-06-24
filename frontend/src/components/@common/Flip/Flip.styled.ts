import styled from '@emotion/styled';

type FlipperProps = {
  flipped: boolean;
  $duration?: number;
};

type FlipWrapperProps = {
  $width?: string;
  $height?: string;
};

const DEFAULT_DURATION = 0.8;

export const FlipWrapper = styled.div<FlipWrapperProps>`
  position: relative;
  perspective: 1000px;
  width: ${({ $width }) => $width || '100%'};
  height: ${({ $height }) => $height || '100%'};
`;

export const Flipper = styled.div<FlipperProps>`
  position: absolute;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  transition: transform ${({ $duration }) => $duration ?? DEFAULT_DURATION}s ease-in-out;
  transform-origin: center;
  transform: ${({ flipped }) => (flipped ? 'rotateY(180deg)' : 'rotateY(0deg)')};
`;

export const InitialView = styled.div`
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  transform: rotateY(0deg);
`;

export const FlippedView = styled.div`
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  transform: rotateY(180deg);
`;
