import { ANIMATION_DURATION, ANIMATION_SCALE } from '@/constants/animation';
import { TouchState } from '@/types/touchState';
import { css } from '@emotion/react';

type ScalePressEffectProps = {
  touchState: TouchState;
  scaleValue?: number;
  duration?: number;
};

export const scalePressEffect = ({
  touchState,
  scaleValue = ANIMATION_SCALE.SCALE_PRESS,
  duration = ANIMATION_DURATION.SCALE_PRESS,
}: ScalePressEffectProps) => {
  const getScale = () => {
    switch (touchState) {
      case 'pressing':
        return scaleValue;
      default:
        return 1;
    }
  };

  return css`
    isolation: isolate;
    transform: scale(${getScale()});
    transition: transform ${duration}ms ease-out;
  `;
};
