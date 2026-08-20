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

const toRadian = (degree: number) => (degree * Math.PI) / 180;

/**
 * 조각 이름을 반경 방향으로 눕히는 배치를 계산한다.
 *
 * 가로로 그리면 글자가 조각의 좁은 쪽(접선)을 쓰게 되어, 이름이 길수록 옆 조각을 침범한다.
 * 반경 방향은 조각이 아무리 얇아도 쓸 수 있는 길이가 LABEL_LENGTH 로 일정하다.
 *
 * 글자 크기는 두 가지에 걸린다.
 *   1. 길이  — 이름 전체가 LABEL_LENGTH 안에 들어와야 한다
 *   2. 두께  — 글자 높이가 조각의 접선 방향 폭을 넘으면 옆 조각을 침범한다
 *
 * 두께는 중심에 가까울수록 좁으므로 글자열의 **안쪽 끝**이 가장 빠듯하다. 그 지점의 반지름은
 * 글자 크기에 따라 달라지므로(글자가 크면 글자열이 길어져 더 안쪽까지 내려온다) 아래처럼 푼다.
 *
 *   fontSize ≤ 2 × (LABEL_RADIUS − nameLength × fontSize ÷ 2) × sin(각도÷2)
 *   ⇒ fontSize ≤ 2 × LABEL_RADIUS × sin(각도÷2) ÷ (1 + nameLength × sin(각도÷2))
 *
 * 결과가 LABEL_MIN_FONT_SIZE 미만이면 읽을 수 없으므로 null 을 돌려주고, 그 조각은 이름 없이
 * 색 띠로만 남는다. 짧은 이름은 더 얇은 조각까지 살아남는다.
 */
export const getSliceLabel = ({ startAngle, endAngle, nameLength }: Props): SliceLabel | null => {
  if (nameLength <= 0) return null;

  const sweep = endAngle - startAngle;
  if (sweep <= 0) return null;

  // 180도를 넘으면 조각이 원의 절반 이상이라 침범할 이웃이 없다. 그 지점에서 두께 제약을 멈춘다
  // (sin 은 180도를 지나면 다시 줄어들어, 혼자 남아 360도가 된 조각의 이름을 없애 버린다)
  const halfSin = Math.sin(toRadian(Math.min(sweep, 180) / 2));
  const byLength = LABEL_LENGTH / nameLength;
  const byThickness = (2 * LABEL_RADIUS * halfSin) / (1 + nameLength * halfSin);
  const fontSize = Math.min(WHEEL_CONFIG.LABEL_MAX_FONT_SIZE, byLength, byThickness);

  if (fontSize < WHEEL_CONFIG.LABEL_MIN_FONT_SIZE) return null;

  const centerAngle = getCenterAngle(startAngle, endAngle);
  // 왼쪽 반원은 그대로 두면 글자가 거꾸로 선다. 180도 돌려 안쪽을 향해 읽게 한다.
  const isLeftHalf = centerAngle >= 180;

  return {
    x: WHEEL_CONFIG.CENTER + (isLeftHalf ? -LABEL_RADIUS : LABEL_RADIUS),
    y: WHEEL_CONFIG.CENTER,
    rotate: isLeftHalf ? centerAngle + 90 : centerAngle - 90,
    fontSize,
  };
};
