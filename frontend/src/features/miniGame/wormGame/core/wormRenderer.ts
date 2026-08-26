import { WormPoint } from '@/types/miniGame/wormGame';
import { Pose, WORM_RULES } from './wormRules';
import { INTERP_DELAY_TICKS, WormStore, WormView } from './wormStore';

/** 메인 뷰 시야 반지름(u). 회전 반경 34~55u 대비 회피 판단에 충분(설계 SSOT) */
const VIEW_RADIUS = 130;
/** 줌아웃 시 아레나 초기 반지름 대비 여유 */
const ZOOM_OUT_MARGIN = 1.12;
/** 궤적 레이어 해상도(px/u). 430px 폰에서 메인 뷰가 ≈1.65px/u 라 2 면 흐려지지 않는다 */
const LAYER_PX_PER_UNIT = 2;
/** 사망 궤적은 벽이 아니므로 완전히 사라진다(데모 1.4s) */
const DEAD_FADE_MS = 1400;
const MINIMAP_SIZE = 96;
const MINIMAP_MARGIN = 12;
/** 카메라·시야 평활 시정수(ms) — 데모 dt*6 / dt*2.5 에 대응 */
const CAMERA_SMOOTH_MS = 160;
const VIEW_SMOOTH_MS = 400;
/** 머리 주변 데드존(u) — 손떨림 지터 방지 */
const DEAD_ZONE_U = 10;
const GLOW_WIDTH_RATIO = 2.4;
const GLOW_ALPHA = 0.28;
const NAME_FONT_PX = 11;

export type WormPalette = {
  background: string;
  arena: string;
  ring: string;
  head: string;
  minimapBg: string;
  viewport: string;
};

type Layer = {
  canvas: HTMLCanvasElement;
  drawn: number;
  deadAt: number | null;
};

/**
 * 캔버스 렌더러. 지렁이당 오프스크린 레이어(≤9)에 확정 궤적을 누적하고 카메라 변환으로 합성한다.
 * 조향은 데모와 같이 매 프레임 "포인터(px) → 월드 → 머리 기준 각도"로 갱신한다(클릭 불필요).
 */
export class WormRenderer {
  private readonly layers = new Map<string, Layer>();
  private camera: WormPoint | null = null;
  private view = VIEW_RADIUS;
  private lastFrameAt = 0;

  constructor(
    private readonly store: WormStore,
    private colorOf: (playerName: string) => string,
    private readonly palette: WormPalette
  ) {}

  /** roster 갱신 시 색 조회만 교체 — 렌더러 인스턴스(레이어·카메라·페이드)는 유지 */
  setColorOf(colorOf: (playerName: string) => string): void {
    this.colorOf = colorOf;
  }

  render(ctx: CanvasRenderingContext2D, width: number, height: number, now: number): void {
    const { store } = this;
    const dt = this.lastFrameAt ? now - this.lastFrameAt : 16;
    this.lastFrameAt = now;

    ctx.fillStyle = this.palette.background;
    ctx.fillRect(0, 0, width, height);
    if (store.initialRadius === 0) return;

    const serverTick = store.serverTickAt(now);
    const poses = new Map<string, Pose>();
    for (const w of store.worms.values()) {
      const pose = store.displayPose(w, now);
      if (pose) poses.set(w.playerName, pose);
      const layer = this.layerFor(w);
      // 자기 지렁이·사망자는 전부 확정, 타인은 보간 지연 이전 점만 확정(머리보다 앞선 궤적이 그려지지 않게)
      const commitTick =
        w.playerName === store.myName || !w.alive ? Infinity : serverTick - INTERP_DELAY_TICKS;
      store.commitUpTo(w, commitTick);
      this.syncLayer(layer, w, now);
    }

    // 카메라·시야: 관전 대상 추적, FINISH 면 전체 맵으로 줌아웃
    const follow = store.zoomOut
      ? undefined
      : (poses.get(store.followName) ?? poses.get(store.myName));
    const target = follow ?? { x: 0, y: 0 };
    const targetView = follow ? VIEW_RADIUS : store.initialRadius * ZOOM_OUT_MARGIN;
    if (!this.camera) this.camera = { x: target.x, y: target.y };
    const kCam = 1 - Math.exp(-dt / CAMERA_SMOOTH_MS);
    const kView = 1 - Math.exp(-dt / VIEW_SMOOTH_MS);
    this.camera.x += (target.x - this.camera.x) * kCam;
    this.camera.y += (target.y - this.camera.y) * kCam;
    this.view += (targetView - this.view) * kView;
    const scale = Math.min(width, height) / (2 * this.view);

    this.steerFromPointer(poses, width, height, scale);

    ctx.save();
    ctx.translate(width / 2, height / 2);
    ctx.scale(scale, scale);
    ctx.translate(-this.camera.x, -this.camera.y);

    ctx.beginPath();
    ctx.arc(0, 0, store.radius, 0, Math.PI * 2);
    ctx.fillStyle = this.palette.arena;
    ctx.fill();
    ctx.lineWidth = 2.5 / scale;
    ctx.strokeStyle = this.palette.ring;
    ctx.stroke();
    ctx.clip();

    const R0 = store.initialRadius;
    for (const w of store.worms.values()) {
      const layer = this.layers.get(w.playerName)!;
      const alpha = this.alphaOf(layer, now);
      if (alpha <= 0) continue;
      const color = this.colorOf(w.playerName);
      const pose = poses.get(w.playerName);
      const tail = w.trail[w.trail.length - 1];

      ctx.globalAlpha = alpha;
      ctx.drawImage(layer.canvas, -R0, -R0, 2 * R0, 2 * R0);
      if (pose && tail) {
        // 확정 궤적 끝 → 머리 한 구간. 남은 pending 은 렌더 시점보다 미래 점이라 그리지 않는다
        ctx.beginPath();
        ctx.moveTo(tail.x, tail.y);
        ctx.lineTo(pose.x, pose.y);
        this.strokeTrail(ctx, color, alpha);
      }
      if (pose && w.alive) {
        ctx.globalAlpha = 1;
        ctx.beginPath();
        ctx.arc(pose.x, pose.y, WORM_RULES.trailRadius * 0.95, 0, Math.PI * 2);
        ctx.fillStyle = this.palette.head;
        ctx.fill();
        ctx.fillStyle = color;
        ctx.font = `${NAME_FONT_PX / scale}px sans-serif`;
        ctx.textAlign = 'center';
        ctx.fillText(w.playerName, pose.x, pose.y - NAME_FONT_PX / scale);
      }
      ctx.globalAlpha = 1;
    }
    ctx.restore();

    this.drawMinimap(ctx, poses, scale, width, height, now);
  }

  /** 포인터(px) → 월드 → 머리 기준 목표각. 마우스 hover·터치 홀드 모두 매 프레임 따라간다 */
  private steerFromPointer(poses: Map<string, Pose>, width: number, height: number, scale: number) {
    const { store } = this;
    const p = store.pointer;
    const me = poses.get(store.myName);
    if (!p || !me || !store.inputEnabled || !this.camera) return;
    const wx = this.camera.x + (p.x - width / 2) / scale;
    const wy = this.camera.y + (p.y - height / 2) / scale;
    if (Math.hypot(wx - me.x, wy - me.y) < DEAD_ZONE_U) return;
    store.steer(Math.atan2(wy - me.y, wx - me.x));
  }

  private drawMinimap(
    ctx: CanvasRenderingContext2D,
    poses: Map<string, WormPoint>,
    mainScale: number,
    viewW: number,
    viewH: number,
    now: number
  ) {
    const { store } = this;
    const size = MINIMAP_SIZE;
    const ox = viewW - size - MINIMAP_MARGIN;
    const oy = MINIMAP_MARGIN;
    const R0 = store.initialRadius;
    const s = size / (2 * R0);
    const cx = ox + size / 2;
    const cy = oy + size / 2;

    ctx.save();
    ctx.beginPath();
    ctx.arc(cx, cy, size / 2, 0, Math.PI * 2);
    ctx.fillStyle = this.palette.minimapBg;
    ctx.fill();
    ctx.beginPath();
    ctx.arc(cx, cy, store.radius * s, 0, Math.PI * 2);
    ctx.strokeStyle = this.palette.ring;
    ctx.lineWidth = 1;
    ctx.stroke();

    for (const w of store.worms.values()) {
      const layer = this.layers.get(w.playerName);
      if (!layer) continue;
      const alpha = this.alphaOf(layer, now);
      if (alpha <= 0) continue;
      ctx.globalAlpha = alpha * 0.75;
      ctx.drawImage(layer.canvas, ox, oy, size, size);
    }
    ctx.globalAlpha = 1;

    if (this.camera) {
      ctx.strokeStyle = this.palette.viewport;
      ctx.strokeRect(
        cx + (this.camera.x - viewW / mainScale / 2) * s,
        cy + (this.camera.y - viewH / mainScale / 2) * s,
        (viewW / mainScale) * s,
        (viewH / mainScale) * s
      );
    }
    for (const w of store.worms.values()) {
      const p = poses.get(w.playerName);
      if (!p || !w.alive) continue;
      const isMe = w.playerName === store.myName;
      ctx.beginPath();
      ctx.arc(cx + p.x * s, cy + p.y * s, isMe ? 3.5 : 2.5, 0, Math.PI * 2);
      ctx.fillStyle = isMe ? this.palette.head : this.colorOf(w.playerName);
      ctx.fill();
      if (isMe) {
        ctx.strokeStyle = this.colorOf(w.playerName);
        ctx.lineWidth = 1.5;
        ctx.stroke();
      }
    }
    ctx.restore();
  }

  private layerFor(w: WormView): Layer {
    const px = Math.ceil(2 * this.store.initialRadius * LAYER_PX_PER_UNIT);
    let layer = this.layers.get(w.playerName);
    // 스냅샷으로 initialRadius 가 넓어졌으면 레이어를 새로 잡고 전체 재작성
    if (layer && layer.canvas.width !== px) {
      layer.canvas.width = px;
      layer.canvas.height = px;
      w.layerDirty = true;
    }
    if (!layer) {
      const canvas = document.createElement('canvas');
      canvas.width = px;
      canvas.height = px;
      layer = { canvas, drawn: 0, deadAt: null };
      this.layers.set(w.playerName, layer);
    }
    return layer;
  }

  /** 레이어에 새로 확정된 궤적 구간을 추가(글로우+코어). 스냅샷 교체 시 전체 재작성 */
  private syncLayer(layer: Layer, w: WormView, now: number) {
    if (!w.alive && layer.deadAt === null) layer.deadAt = now;
    const g = layer.canvas.getContext('2d')!;
    if (w.layerDirty) {
      g.clearRect(0, 0, layer.canvas.width, layer.canvas.height);
      layer.drawn = 0;
      w.layerDirty = false;
    }
    if (w.trail.length === 0 || w.trail.length <= layer.drawn) return;
    const R0 = this.store.initialRadius;
    g.save();
    g.scale(LAYER_PX_PER_UNIT, LAYER_PX_PER_UNIT);
    g.translate(R0, R0);
    g.beginPath();
    const from = Math.max(0, layer.drawn - 1);
    g.moveTo(w.trail[from].x, w.trail[from].y);
    for (let i = from + 1; i < w.trail.length; i++) g.lineTo(w.trail[i].x, w.trail[i].y);
    this.strokeTrail(g, this.colorOf(w.playerName), 1);
    g.restore();
    layer.drawn = w.trail.length;
  }

  /** 데모 스타일: 넓고 옅은 글로우 위에 코어 선 */
  private strokeTrail(ctx: CanvasRenderingContext2D, color: string, alpha: number) {
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.strokeStyle = color;
    ctx.globalAlpha = alpha * GLOW_ALPHA;
    ctx.lineWidth = WORM_RULES.trailRadius * GLOW_WIDTH_RATIO;
    ctx.stroke();
    ctx.globalAlpha = alpha;
    ctx.lineWidth = WORM_RULES.trailRadius;
    ctx.stroke();
  }

  private alphaOf(layer: Layer, now: number): number {
    if (layer.deadAt === null) return 1;
    return Math.max(0, 1 - (now - layer.deadAt) / DEAD_FADE_MS);
  }
}
