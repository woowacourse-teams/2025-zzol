import { css } from '@emotion/react';
import { TouchState } from '@/types/touchState';
import { backgroundPressEffect } from './effects/backGroundPressEffect';
import { scalePressEffect } from './effects/scalePressEffect';
import { ANIMATION_SCALE } from '@/constants/animation';

type Props = {
  activeColor: string;
  touchState: TouchState;
  enableScale?: boolean;
  scaleValue?: number;
};

export const buttonHoverPress = ({
  activeColor,
  touchState,
  enableScale = true,
  scaleValue = ANIMATION_SCALE.SCALE_PRESS,
}: Props) => {
  return css`
    /* 데스크톱: hover 효과 */
    @media (hover: hover) and (pointer: fine) {
      &:hover {
        background-color: ${activeColor};
      }
    }

    /* 터치 디바이스: touchState 상태로 제어 */
    ${backgroundPressEffect({ activeColor, touchState })}
    ${enableScale && scalePressEffect({ touchState, scaleValue })}
  `;
};
