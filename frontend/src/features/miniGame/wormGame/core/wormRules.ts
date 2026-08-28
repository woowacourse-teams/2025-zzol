/**
 * 서버 `WormGameRulesProperties` 의 @DefaultValue 클라이언트 미러 — 자기 지렁이 클라 예측·타인 외삽 전용.
 * 판정은 전부 서버가 한다. 값이 바뀌면 BE 와 함께 고친다.
 */
export const WORM_RULES = {
  tickMillis: 50,
  baseSpeed: 120,
  maxSpeedMultiplier: 1.6,
  speedRampTicks: 1200,
  omegaBaseRadians: (200 * Math.PI) / 180,
  omegaExponent: 0.7,
  trailRadius: 6,
} as const;

export const normalizeAngle = (a: number): number => {
  let r = a % (2 * Math.PI);
  if (r <= -Math.PI) r += 2 * Math.PI;
  if (r > Math.PI) r -= 2 * Math.PI;
  return r;
};

export const speedUnitsPerSecond = (tick: number): number => {
  const ramp = Math.min(1, tick / WORM_RULES.speedRampTicks);
  return WORM_RULES.baseSpeed * (1 + ramp * (WORM_RULES.maxSpeedMultiplier - 1));
};

export const speedPerTick = (tick: number): number =>
  (speedUnitsPerSecond(tick) * WORM_RULES.tickMillis) / 1000;

export const omegaPerTick = (tick: number): number => {
  const scale = (speedUnitsPerSecond(tick) / WORM_RULES.baseSpeed) ** WORM_RULES.omegaExponent;
  return (WORM_RULES.omegaBaseRadians * scale * WORM_RULES.tickMillis) / 1000;
};

export type Pose = { x: number; y: number; angle: number };

/** 서버 `Worm.advance` 1틱: 목표각으로 상한 내 회전 후 전진. */
export const advance = (pose: Pose, targetAngle: number, tick: number): Pose => {
  const diff = normalizeAngle(targetAngle - pose.angle);
  const maxTurn = omegaPerTick(tick);
  const angle = normalizeAngle(pose.angle + Math.max(-maxTurn, Math.min(maxTurn, diff)));
  const d = speedPerTick(tick);
  return { x: pose.x + Math.cos(angle) * d, y: pose.y + Math.sin(angle) * d, angle };
};

export const lerpAngle = (a: number, b: number, t: number): number =>
  normalizeAngle(a + normalizeAngle(b - a) * t);
