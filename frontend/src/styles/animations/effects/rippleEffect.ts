import { ANIMATION_DURATION } from '@/constants/animation';
import { TouchState } from '@/types/touchState';
import { theme } from '@/styles/theme';
import { css } from '@emotion/react';
import { Z_INDEX } from '@/constants/zIndex';

type Props = {
  touchState: TouchState;
  diameter?: string;
};

export const rippleEffect = ({ touchState, diameter = '30px' }: Props) => {
  const rippleColor = theme.color.gray[200];

  const getRippleScale = () => {
    switch (touchState) {
      case 'pressing':
        return 1;
      case 'releasing':
        return 1;
      default:
        return 0;
    }
  };

  const getRippleOpacity = () => {
    switch (touchState) {
      case 'pressing':
        return 1;
      case 'releasing':
        return 0.5;
      default:
        return 0;
    }
  };

  return css`
    isolation: isolate;

    &::before {
      z-index: ${Z_INDEX.RIPPLE_Effect};
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      width: ${diameter};
      height: ${diameter};
      background: ${rippleColor};
      border-radius: 50%;

      transform: translate(-50%, -50%) scale(${getRippleScale()});
      opacity: ${getRippleOpacity()};
      transition:
        transform ${ANIMATION_DURATION.RIPPLE}ms ease-out,
        opacity ${ANIMATION_DURATION.RIPPLE}ms ease-out;
    }
  `;
};
