import { WormDeltaMessage, WormPoint, WormSnapshotMessage } from '@/types/miniGame/wormGame';
import { advance, lerpAngle, Pose, speedPerTick, WORM_RULES } from './wormRules';

/**
 * 렌더 스토어 — React state 밖. 20Hz 델타를 그대로 쌓고 rAF 루프가 읽는다(메시지당 setState 금지).
 *
 * 규칙(설계 SSOT):
 *  - 델타는 tick 단조증가가 아니면 폐기. 스냅샷은 전체 교체하고 그 tick 이전 델타를 폐기.
 *  - 자기 지렁이: 클라 예측 — 서버 최신 포즈에서 현재 서버 tick 까지 로컬 목표각으로 재적분.
 *  - 타인: 보간 버퍼 INTERP_DELAY_TICKS 뒤를 렌더, 버퍼가 비면 현재 각도로 외삽(상한 EXTRAPOLATE_MAX_TICKS).
 */
export const INTERP_DELAY_TICKS = 2; // 100ms
const EXTRAPOLATE_MAX_TICKS = 3;
/** 자기 예측 상한(300ms). 넘으면 서버 틱이 멈춘 것(PREPARE·FINISH·끊김) — 더 밀지 않는다 */
export const PREDICT_MAX_TICKS = 6;
const SAMPLE_KEEP = 8;

type Sample = Pose & { tick: number };
type PendingPoint = WormPoint & { tick: number };

export type WormView = {
  playerName: string;
  alive: boolean;
  /** 화면·레이어에 확정된 궤적 */
  trail: WormPoint[];
  /** 아직 렌더 시각이 안 된 점(타인 보간 지연분). 렌더 tick 이 지나면 trail 로 옮긴다 */
  pending: PendingPoint[];
  samples: Sample[];
  /** 스냅샷 교체 등으로 trail 전체를 다시 그려야 할 때 true — 렌더러가 소비 후 내린다 */
  layerDirty: boolean;
};

export class WormStore {
  tick = -1;
  radius = 0;
  /** 최초 관측 반지름(= 아레나 초기 반지름). 레이어 크기·미니맵 기준 */
  initialRadius = 0;
  tickMillis: number = WORM_RULES.tickMillis;
  readonly worms = new Map<string, WormView>();

  myName: string;
  /** 관전 대상. 기본은 자신 */
  followName: string;
  /** 로컬 조향 목표각(예측 입력). 조작 계층이 갱신 */
  targetAngle: number | null = null;

  /** tick 이 마지막으로 갱신된 로컬 시각 */
  private anchorAt = 0;

  constructor(myName: string) {
    this.myName = myName;
    this.followName = myName;
  }

  /** 조작 계층이 목표각을 쓴다(React 컴파일러 린트가 훅 인자 속성 대입을 막아 메서드로) */
  steer(angle: number): void {
    this.targetAngle = angle;
  }

  follow(playerName: string): void {
    this.followName = playerName;
  }

  /** tick↔로컬 시계 매핑. 최신 델타 도착 시각을 앵커로 쓴다 */
  // ponytail: 앵커=최신 델타 도착 시각(지터 ±20ms). 튀면 max-기반 평활로 교체
  serverTickAt(now: number): number {
    if (this.tick < 0) return 0;
    return this.tick + (now - this.anchorAt) / this.tickMillis;
  }

  applyDelta(delta: WormDeltaMessage, now: number): boolean {
    if (delta.tick <= this.tick) return false;
    this.tick = delta.tick;
    this.anchorAt = now;
    this.setRadius(delta.radius);

    for (const p of delta.worms) {
      const w = this.getOrCreate(p.playerName);
      const wasAlive = w.alive;
      w.alive = p.alive;
      if (wasAlive || w.samples.length === 0) {
        w.samples.push({ tick: delta.tick, x: p.x, y: p.y, angle: p.angle });
        if (w.samples.length > SAMPLE_KEEP) w.samples.shift();
        w.pending.push({ tick: delta.tick, x: p.x, y: p.y });
      }
    }
    return true;
  }

  applySnapshot(snapshot: WormSnapshotMessage, now: number): void {
    this.tick = snapshot.tick;
    this.tickMillis = snapshot.tickMillis;
    this.anchorAt = now;
    this.setRadius(snapshot.radius);

    const seen = new Set<string>();
    for (const s of snapshot.worms) {
      seen.add(s.playerName);
      const w = this.getOrCreate(s.playerName);
      w.alive = s.alive;
      w.trail = s.trail.slice();
      w.pending = [];
      w.layerDirty = true;
      // 축소 뒤 재접속하면 스냅샷 radius 가 이미 줄어 있다 — 궤적 범위까지 넓혀 초반 궤적이 잘리지 않게
      for (const p of s.trail) {
        this.initialRadius = Math.max(this.initialRadius, Math.abs(p.x), Math.abs(p.y));
      }
      const head = s.trail[s.trail.length - 1];
      const prev = s.trail[s.trail.length - 2];
      if (head) {
        // 스냅샷에는 각도가 없다. 1점(PREPARE 스폰)이면 0 으로 확정하지 않고 기존 샘플 각도를 유지
        const angle = prev
          ? Math.atan2(head.y - prev.y, head.x - prev.x)
          : (w.samples[w.samples.length - 1]?.angle ?? 0);
        w.samples = [{ tick: snapshot.tick, x: head.x, y: head.y, angle }];
      }
    }
    for (const name of this.worms.keys()) if (!seen.has(name)) this.worms.delete(name);
  }

  /** pending 중 tick ≤ upTo 인 점을 trail 로 확정. 렌더러가 매 프레임 호출 */
  commitUpTo(w: WormView, upTo: number): number {
    let n = 0;
    while (w.pending.length && w.pending[0].tick <= upTo) {
      const p = w.pending.shift()!;
      w.trail.push({ x: p.x, y: p.y });
      n++;
    }
    return n;
  }

  /** 이 프레임에 그릴 머리 포즈. 자기 지렁이는 예측, 타인은 보간·외삽 */
  displayPose(w: WormView, now: number): Pose | null {
    const latest = w.samples[w.samples.length - 1];
    if (!latest) return null;
    if (!w.alive) return latest;
    const serverTick = this.serverTickAt(now);
    return w.playerName === this.myName
      ? this.predict(latest, serverTick)
      : this.interpolate(w.samples, serverTick - INTERP_DELAY_TICKS);
  }

  /** 자기 지렁이: 서버 확정 포즈에서 현재 서버 tick 까지 로컬 목표각으로 재적분 */
  // ponytail: 입력 큐 리플레이 대신 "마지막 목표각"만 적용 — 조향은 last-wins 라 lag 창 내 변경만 오차
  private predict(latest: Sample, serverTick: number): Pose {
    const target = this.targetAngle ?? latest.angle;
    const capped = Math.min(serverTick, latest.tick + PREDICT_MAX_TICKS);
    let pose: Pose = latest;
    let t = latest.tick;
    const end = Math.floor(capped);
    for (; t < end; t++) pose = advance(pose, target, t);
    const frac = capped - Math.max(end, latest.tick);
    if (frac > 0) {
      const d = speedPerTick(t) * frac;
      pose = {
        ...pose,
        x: pose.x + Math.cos(pose.angle) * d,
        y: pose.y + Math.sin(pose.angle) * d,
      };
    }
    return pose;
  }

  private interpolate(samples: Sample[], renderTick: number): Pose {
    const latest = samples[samples.length - 1];
    if (renderTick >= latest.tick) {
      const ahead = Math.min(renderTick - latest.tick, EXTRAPOLATE_MAX_TICKS);
      const d = speedPerTick(latest.tick) * ahead;
      return {
        x: latest.x + Math.cos(latest.angle) * d,
        y: latest.y + Math.sin(latest.angle) * d,
        angle: latest.angle,
      };
    }
    for (let i = samples.length - 2; i >= 0; i--) {
      const a = samples[i];
      const b = samples[i + 1];
      if (renderTick >= a.tick) {
        const t = (renderTick - a.tick) / (b.tick - a.tick);
        return {
          x: a.x + (b.x - a.x) * t,
          y: a.y + (b.y - a.y) * t,
          angle: lerpAngle(a.angle, b.angle, t),
        };
      }
    }
    return samples[0];
  }

  private setRadius(radius: number) {
    this.radius = radius;
    if (this.initialRadius === 0) this.initialRadius = radius;
  }

  private getOrCreate(name: string): WormView {
    let w = this.worms.get(name);
    if (!w) {
      w = { playerName: name, alive: true, trail: [], pending: [], samples: [], layerDirty: true };
      this.worms.set(name, w);
    }
    return w;
  }
}
