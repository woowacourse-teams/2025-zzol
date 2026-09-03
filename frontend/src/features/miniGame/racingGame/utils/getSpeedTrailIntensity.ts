import { MAX_SPEED, MIN_SPEED } from '../constants/track';

/**
 * 속도선의 진하기를 0~1 로 낸다.
 *
 * 최저 속도는 안 눌러도 나오는 값이라 0이다. 그 위로만 효과가 붙어 연타가 화면에 드러난다.
 * 굴러가는 표현인 회전과 배경 흐름은 최저 속도에서도 그대로 돈다. 실제로 앞으로 가고 있어서다.
 */
export const getSpeedTrailIntensity = (speed: number): number => {
  const range = MAX_SPEED - MIN_SPEED;
  return Math.min(Math.max((speed - MIN_SPEED) / range, 0), 1);
};
