import { WormDeltaMessage, WormSnapshotMessage } from '@/types/miniGame/wormGame';
import { advance, speedPerTick } from './wormRules';
import { INTERP_DELAY_TICKS, WormStore } from './wormStore';

const delta = (
  tick: number,
  worms: Partial<WormDeltaMessage['worms'][number]>[]
): WormDeltaMessage => ({
  tick,
  radius: 220,
  worms: worms.map((w) => ({
    playerName: 'me',
    x: 0,
    y: 0,
    angle: 0,
    alive: true,
    lastSeq: 0,
    ...w,
  })),
});

const snapshot = (tick: number): WormSnapshotMessage => ({
  tick,
  tickMillis: 50,
  serverNow: '2026-08-26T00:00:00Z',
  radius: 200,
  worms: [
    {
      playerName: 'me',
      alive: true,
      trail: [
        { x: 0, y: 0 },
        { x: 10, y: 0 },
      ],
    },
    { playerName: 'other', alive: false, trail: [{ x: 5, y: 5 }] },
  ],
});

describe('WormStore — 델타·스냅샷 규칙', () => {
  it('tick 이 단조증가하지 않는 델타는 폐기한다', () => {
    const s = new WormStore('me');
    expect(s.applyDelta(delta(10, [{}]), 0)).toBe(true);
    expect(s.applyDelta(delta(10, [{ x: 99 }]), 50)).toBe(false);
    expect(s.applyDelta(delta(9, [{ x: 99 }]), 50)).toBe(false);
    expect(s.worms.get('me')!.pending).toHaveLength(1);
  });

  it('스냅샷은 궤적을 전체 교체하고 그 tick 이전 델타를 폐기한다', () => {
    const s = new WormStore('me');
    s.applyDelta(delta(5, [{ playerName: 'gone' }]), 0);
    s.applySnapshot(snapshot(100), 0);
    expect(s.worms.has('gone')).toBe(false);
    expect(s.worms.get('me')!.trail).toHaveLength(2);
    expect(s.worms.get('me')!.layerDirty).toBe(true);
    expect(s.worms.get('other')!.alive).toBe(false);
    expect(s.applyDelta(delta(99, [{}]), 10)).toBe(false);
    expect(s.applyDelta(delta(101, [{}]), 10)).toBe(true);
  });

  it('commitUpTo 는 렌더 tick 이 지난 점만 trail 로 옮긴다', () => {
    const s = new WormStore('me');
    s.applyDelta(delta(1, [{ playerName: 'o', x: 1 }]), 0);
    s.applyDelta(delta(2, [{ playerName: 'o', x: 2 }]), 50);
    s.applyDelta(delta(3, [{ playerName: 'o', x: 3 }]), 100);
    const w = s.worms.get('o')!;
    expect(s.commitUpTo(w, 2)).toBe(2);
    expect(w.trail.map((p) => p.x)).toEqual([1, 2]);
    expect(w.pending.map((p) => p.x)).toEqual([3]);
  });
});

describe('WormStore — 자기 예측·타인 보간', () => {
  it('자기 지렁이는 서버 포즈에서 현재 서버 tick 까지 목표각으로 재적분한다', () => {
    const s = new WormStore('me');
    s.applyDelta(delta(10, [{ x: 0, y: 0, angle: 0 }]), 1000);
    s.targetAngle = Math.PI / 2;
    // 2틱(100ms) 뒤
    const pose = s.displayPose(s.worms.get('me')!, 1100)!;
    let expected = { x: 0, y: 0, angle: 0 };
    expected = advance(expected, Math.PI / 2, 10);
    expected = advance(expected, Math.PI / 2, 11);
    expect(pose.x).toBeCloseTo(expected.x);
    expect(pose.y).toBeCloseTo(expected.y);
    expect(pose.angle).toBeCloseTo(expected.angle);
  });

  it('타인은 INTERP_DELAY_TICKS 뒤 시점을 두 샘플 사이에서 선형 보간한다', () => {
    const s = new WormStore('me');
    s.applyDelta(delta(10, [{ playerName: 'o', x: 0 }]), 0);
    s.applyDelta(delta(12, [{ playerName: 'o', x: 20 }]), 100);
    // 최신 델타 도착 직후 + 1틱 → 렌더 tick = 13 - 2 = 11 → x = 10
    const pose = s.displayPose(s.worms.get('o')!, 100 + 50)!;
    expect(INTERP_DELAY_TICKS).toBe(2);
    expect(pose.x).toBeCloseTo(10);
  });

  it('버퍼가 비면 현재 각도로 외삽하되 3틱을 넘지 않는다', () => {
    const s = new WormStore('me');
    s.applyDelta(delta(10, [{ playerName: 'o', x: 0, angle: 0 }]), 0);
    const w = s.worms.get('o')!;
    // 렌더 tick = 10 + 20 - 2 = 28 → 18틱 앞이지만 3틱으로 상한
    const pose = s.displayPose(w, 20 * 50)!;
    expect(pose.x).toBeCloseTo(speedPerTick(10) * 3);
  });

  it('사망자는 마지막 서버 포즈에 고정된다', () => {
    const s = new WormStore('me');
    s.applyDelta(delta(10, [{ playerName: 'o', x: 7 }]), 0);
    s.applyDelta(delta(11, [{ playerName: 'o', x: 8, alive: false }]), 50);
    s.applyDelta(delta(12, [{ playerName: 'o', x: 999, alive: false }]), 100);
    const w = s.worms.get('o')!;
    expect(s.displayPose(w, 5000)!.x).toBe(8);
    expect(w.pending.map((p) => p.x)).toEqual([7, 8]);
  });
});
