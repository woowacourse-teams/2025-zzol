import { ANIMATION_DURATION } from '@/constants/animation';
import { TouchState } from '@/types/touchState';
import { css } from '@emotion/react';

type Props = {
  activeColor: string;
  touchState: TouchState;
};

export const backgroundPressEffect = ({ activeColor, touchState }: Props) => {
  const getScaleX = () => {
    switch (touchState) {
      case 'pressing':
        return 1.2;
      case 'releasing':
        return 1;
      default:
        return 0;
    }
  };

  const getOpacity = () => {
    switch (touchState) {
      case 'pressing':
        return 1;
      case 'releasing':
        return 0.5; // 릴리즈 시 투명도 감소
      default:
        return 0;
    }
  };

  const shouldAnimate = touchState !== 'idle';

  return css`
    position: relative;
    overflow: hidden;
    isolation: isolate;

    &::before {
      z-index: -1;
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: ${activeColor};
      border-radius: 12px;

      transform: scaleX(${getScaleX()});
      opacity: ${getOpacity()};
      transform-origin: center;

      transition:
        ${shouldAnimate ? `transform ${ANIMATION_DURATION.BACKGROUND_PRESS}ms ease-out` : 'none'},
        opacity ${ANIMATION_DURATION.BACKGROUND_PRESS}ms ease-out;
    }
  `;
};
