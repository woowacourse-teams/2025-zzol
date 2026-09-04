import { MAX_SPEED, MIN_SPEED } from '../constants/track';
import { getSpeedTrailIntensity } from './getSpeedTrailIntensity';

describe('getSpeedTrailIntensity', () => {
  it('최저 속도에서는 효과를 켜지 않는다', () => {
    expect(getSpeedTrailIntensity(MIN_SPEED)).toBe(0);
  });

  it('최저 속도 아래로 내려가도 음수가 되지 않는다', () => {
    expect(getSpeedTrailIntensity(0)).toBe(0);
  });

  it('최저 속도를 넘으면 효과가 켜진다', () => {
    expect(getSpeedTrailIntensity(MIN_SPEED + 1)).toBeGreaterThan(0);
  });

  it('빠를수록 진해진다', () => {
    expect(getSpeedTrailIntensity(40)).toBeGreaterThan(getSpeedTrailIntensity(20));
  });

  it('최고 속도에서 1이고 그 위로는 안 넘는다', () => {
    expect(getSpeedTrailIntensity(MAX_SPEED)).toBe(1);
    expect(getSpeedTrailIntensity(MAX_SPEED + 50)).toBe(1);
  });
});
