import {
  advance,
  normalizeAngle,
  omegaPerTick,
  speedPerTick,
  speedUnitsPerSecond,
} from './wormRules';

// 서버 WormGameRules.defaults() 와 같은 값이 나와야 예측이 드리프트하지 않는다.
describe('wormRules — 서버 미러', () => {
  it('속도는 120 에서 1200틱에 걸쳐 192 로 선형 증가하고 그 뒤 고정', () => {
    expect(speedUnitsPerSecond(0)).toBe(120);
    expect(speedUnitsPerSecond(600)).toBeCloseTo(156);
    expect(speedUnitsPerSecond(1200)).toBeCloseTo(192);
    expect(speedUnitsPerSecond(5000)).toBeCloseTo(192);
    expect(speedPerTick(0)).toBeCloseTo(6);
    expect(speedPerTick(1200)).toBeCloseTo(9.6);
  });

  it('회전 상한은 200°/s × (v/120)^0.7 — 최고속에서 ≈278°/s', () => {
    expect((omegaPerTick(0) * 20 * 180) / Math.PI).toBeCloseTo(200, 5);
    expect((omegaPerTick(1200) * 20 * 180) / Math.PI).toBeCloseTo(200 * 1.6 ** 0.7, 5);
  });

  it('advance 는 목표각으로 상한만큼만 돌고 전진한다', () => {
    const pose = advance({ x: 0, y: 0, angle: 0 }, Math.PI, 0);
    expect(pose.angle).toBeCloseTo(omegaPerTick(0));
    expect(Math.hypot(pose.x, pose.y)).toBeCloseTo(speedPerTick(0));
  });

  it('normalizeAngle 은 (-π, π] 로 접는다', () => {
    expect(normalizeAngle(Math.PI * 3)).toBeCloseTo(Math.PI);
    expect(normalizeAngle(-Math.PI)).toBeCloseTo(Math.PI);
    expect(normalizeAngle(0.5)).toBeCloseTo(0.5);
  });
});
