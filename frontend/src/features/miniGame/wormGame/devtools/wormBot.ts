import { normalizeAngle, WORM_RULES } from '../core/wormRules';
import { WormStore } from '../core/wormStore';

/**
 * dev autoTest 봇 두뇌 — 설계 데모의 봇을 실제 스토어 위로 옮긴 것.
 * 전방을 몇 걸음 샘플링해 궤적·경계에 부딪히지 않는 첫 후보 방향을 고른다. 판정은 서버가 하므로 근사면 충분.
 */
const LOOKAHEAD_U = 70;
const LOOKAHEAD_STEPS = 6;
/** 궤적 두께 + 여유 */
const CLEARANCE_U = WORM_RULES.trailRadius * 2 + 6;
/** 자기 궤적 끝(머리 바로 뒤)은 항상 가까우니 판정에서 뺀다 */
const SELF_SKIP_POINTS = 8;
/** 후보 방향(현재 진행각 기준 오프셋, rad). 앞이 막히면 좌우로 점점 크게 꺾는다 */
const CANDIDATE_OFFSETS = [0, -0.55, 0.55, -1.2, 1.2, -1.9, 1.9];
const EDGE_RATIO = 0.72;

const isSafeAhead = (store: WormStore, x: number, y: number, heading: number): boolean => {
  const me = store.worms.get(store.myName);
  for (let i = 1; i <= LOOKAHEAD_STEPS; i++) {
    const d = (LOOKAHEAD_U * i) / LOOKAHEAD_STEPS;
    const px = x + Math.cos(heading) * d;
    const py = y + Math.sin(heading) * d;
    if (Math.hypot(px, py) > store.radius - CLEARANCE_U) return false;
    for (const w of store.worms.values()) {
      const points = [...w.trail, ...w.pending];
      const end = w === me ? Math.max(0, points.length - SELF_SKIP_POINTS) : points.length;
      for (let k = 0; k < end; k++) {
        if (Math.hypot(points[k].x - px, points[k].y - py) < CLEARANCE_U) return false;
      }
    }
  }
  return true;
};

/** 다음 목표각. 자기 지렁이 정보가 없으면 null */
export const chooseHeading = (store: WormStore): number | null => {
  const me = store.worms.get(store.myName);
  const latest = me?.samples[me.samples.length - 1];
  if (!me || !latest || !me.alive) return null;

  // 가장자리(축소 링 근처)면 우선 중심 쪽으로
  if (Math.hypot(latest.x, latest.y) > store.radius * EDGE_RATIO) {
    const toCenter = Math.atan2(-latest.y, -latest.x);
    for (const offset of [0, -0.5, 0.5, -1.0, 1.0]) {
      if (isSafeAhead(store, latest.x, latest.y, toCenter + offset)) return toCenter + offset;
    }
  }
  for (const offset of CANDIDATE_OFFSETS) {
    const heading = normalizeAngle(latest.angle + offset);
    if (isSafeAhead(store, latest.x, latest.y, heading))
      return heading + (Math.random() - 0.5) * 0.2;
  }
  // 전부 막힘 — 마지막 몸부림
  return normalizeAngle(latest.angle + (Math.random() - 0.5) * 2.4);
};
