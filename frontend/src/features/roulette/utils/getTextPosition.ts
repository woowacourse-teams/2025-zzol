import { WHEEL_CONFIG } from '../constants/config';
import { polarToCartesian } from './index';

export const getTextPosition = (centerAngle: number, radius: number = WHEEL_CONFIG.TEXT_RADIUS) => {
  return polarToCartesian({
    cx: WHEEL_CONFIG.CENTER,
    cy: WHEEL_CONFIG.CENTER,
    r: radius,
    angle: centerAngle,
  });
};
