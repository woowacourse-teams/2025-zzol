import { LABEL_LENGTH, LABEL_RADIUS, WHEEL_CONFIG } from '../constants/config';
import { getCenterAngle } from './getCenterAngle';

export type SliceLabel = {
  x: number;
  y: number;
  rotate: number;
  fontSize: number;
};

type Props = {
  startAngle: number;
  endAngle: number;
  nameLength: number;
};

/**
 * 조각 이름을 반경 방향으로 눕히는 배치를 계산한다.
 *
 * 가로로 그리면 글자가 조각의 좁은 쪽(접선)을 쓰게 되어, 이름이 길수록 옆 조각을 침범한다.
 * 반경 방향은 조각이 아무리 얇아도 쓸 수 있는 길이가 LABEL_LENGTH 로 일정하다.
 *
 * 조각이 MIN_LABEL_ANGLE 보다 얇으면 null 을 돌려준다 — 그 조각은 이름을 그리지 않고
 * 화면 아래 확률 리스트가 대신 읽어준다.
 */
export const getSliceLabel = ({ startAngle, endAngle, nameLength }: Props): SliceLabel | null => {
  if (nameLength === 0) return null;
  if (endAngle - startAngle < WHEEL_CONFIG.MIN_LABEL_ANGLE) return null;

  const centerAngle = getCenterAngle(startAngle, endAngle);
  // 왼쪽 반원은 그대로 두면 글자가 거꾸로 선다. 180도 돌려 안쪽을 향해 읽게 한다.
  const isLeftHalf = centerAngle >= 180;

  return {
    x: WHEEL_CONFIG.CENTER + (isLeftHalf ? -LABEL_RADIUS : LABEL_RADIUS),
    y: WHEEL_CONFIG.CENTER,
    rotate: isLeftHalf ? centerAngle + 90 : centerAngle - 90,
    fontSize: Math.min(WHEEL_CONFIG.LABEL_MAX_FONT_SIZE, LABEL_LENGTH / nameLength),
  };
};
