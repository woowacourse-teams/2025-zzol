import { WormPoint } from '@/types/miniGame/wormGame';
import { WORM_RULES } from './wormRules';
import { INTERP_DELAY_TICKS, WormStore, WormView } from './wormStore';

/** 메인 뷰 시야 반지름(u). 회전 반경 34~55u 대비 회피 판단에 충분(설계 SSOT) */
export const VIEW_RADIUS = 130;
/** 궤적 레이어 해상도(px/u). 430px 폰에서 메인 뷰가 ≈1.65px/u 라 2 면 흐려지지 않는다 */
const LAYER_PX_PER_UNIT = 2;
const DEAD_FADE_MS = 1500;
const DEAD_MIN_ALPHA = 0.35;
const MINIMAP_SIZE = 96;
const MINIMAP_MARGIN = 12;
const CAMERA_SMOOTH_MS = 80;

export type WormPalette = {
  background: string;
  arena: string;
  ring: string;
  dead: string;
  minimapBg: string;
  viewport: string;
};

type Layer = {
  canvas: HTMLCanvasElement;
  drawn: number;
  grayed: boolean;
  deadAt: number | null;
};

/**
 * 캔버스 렌더러. 지렁이당 오프스크린 레이어(≤9)에 확정 궤적을 누적하고 카메라 변환으로 합성한다.
 * 사망은 레이어를 회색으로 1회 다시 그리고 합성 알파로 페이드(연출 전용, 판정은 서버).
 */
export class WormRenderer {
  private readonly layers = new Map<string, Layer>();
  private camera: WormPoint | null = null;
  private lastFrameAt = 0;

  constructor(
    private readonly store: WormStore,
    private readonly colorOf: (playerName: string) => string,
    private readonly palette: WormPalette
  ) {}

  render(ctx: CanvasRenderingContext2D, width: number, height: number, now: number): void {
    const { store } = this;
    const dt = this.lastFrameAt ? now - this.lastFrameAt : 16;
    this.lastFrameAt = now;

    ctx.fillStyle = this.palette.background;
    ctx.fillRect(0, 0, width, height);
    if (store.initialRadius === 0) return;

    const serverTick = store.serverTickAt(now);
    const poses = new Map<string, { x: number; y: number; angle: number }>();
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

    const target = poses.get(store.followName) ?? poses.get(store.myName) ?? { x: 0, y: 0 };
    if (!this.camera) this.camera = { x: target.x, y: target.y };
    const k = 1 - Math.exp(-dt / CAMERA_SMOOTH_MS);
    this.camera.x += (target.x - this.camera.x) * k;
    this.camera.y += (target.y - this.camera.y) * k;

    const scale = Math.min(width, height) / (2 * VIEW_RADIUS);
    ctx.save();
    ctx.translate(width / 2, height / 2);
    ctx.scale(scale, scale);
    ctx.translate(-this.camera.x, -this.camera.y);

    ctx.beginPath();
    ctx.arc(0, 0, store.radius, 0, Math.PI * 2);
    ctx.fillStyle = this.palette.arena;
    ctx.fill();
    ctx.lineWidth = 2 / scale;
    ctx.strokeStyle = this.palette.ring;
    ctx.stroke();

    const R0 = store.initialRadius;
    for (const w of store.worms.values()) {
      const layer = this.layers.get(w.playerName)!;
      ctx.globalAlpha = this.alphaOf(layer, now);
      ctx.drawImage(layer.canvas, -R0, -R0, 2 * R0, 2 * R0);

      const color = w.alive ? this.colorOf(w.playerName) : this.palette.dead;
      const pose = poses.get(w.playerName);
      const tail = w.trail[w.trail.length - 1];
      if (pose && tail) {
        // 확정 궤적 끝 → 보류 점들 → 머리: 레이어에 없는 최신 구간을 매 프레임 폴리라인으로 잇는다
        ctx.beginPath();
        ctx.moveTo(tail.x, tail.y);
        for (const p of w.pending) ctx.lineTo(p.x, p.y);
        ctx.lineTo(pose.x, pose.y);
        this.strokeTrail(ctx, color);
      }
      if (pose) {
        ctx.beginPath();
        ctx.arc(pose.x, pose.y, WORM_RULES.trailRadius * 1.4, 0, Math.PI * 2);
        ctx.fillStyle = color;
        ctx.fill();
      }
      ctx.globalAlpha = 1;
    }
    ctx.restore();

    this.drawMinimap(ctx, width, poses, scale, width, height);
  }

  private drawMinimap(
    ctx: CanvasRenderingContext2D,
    canvasWidth: number,
    poses: Map<string, { x: number; y: number }>,
    mainScale: number,
    viewW: number,
    viewH: number
  ) {
    const { store } = this;
    const size = MINIMAP_SIZE;
    const ox = canvasWidth - size - MINIMAP_MARGIN;
    const oy = MINIMAP_MARGIN;
    const s = size / (2 * store.initialRadius);
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
      if (!p) continue;
      ctx.beginPath();
      ctx.arc(
        cx + p.x * s,
        cy + p.y * s,
        w.playerName === store.myName ? 3.5 : 2.5,
        0,
        Math.PI * 2
      );
      ctx.fillStyle = w.alive ? this.colorOf(w.playerName) : this.palette.dead;
      ctx.fill();
    }
    ctx.restore();
  }

  private layerFor(w: WormView): Layer {
    let layer = this.layers.get(w.playerName);
    if (!layer) {
      const canvas = document.createElement('canvas');
      const px = Math.ceil(2 * this.store.initialRadius * LAYER_PX_PER_UNIT);
      canvas.width = px;
      canvas.height = px;
      layer = { canvas, drawn: 0, grayed: false, deadAt: null };
      this.layers.set(w.playerName, layer);
    }
    return layer;
  }

  /** 레이어에 새로 확정된 궤적 구간을 추가. 사망·스냅샷 교체 시 전체 재작성 */
  private syncLayer(layer: Layer, w: WormView, now: number) {
    if (!w.alive && layer.deadAt === null) layer.deadAt = now;
    const needsGray = !w.alive && !layer.grayed;
    if (w.layerDirty || needsGray) {
      const g = layer.canvas.getContext('2d')!;
      g.clearRect(0, 0, layer.canvas.width, layer.canvas.height);
      layer.drawn = 0;
      layer.grayed = !w.alive;
      w.layerDirty = false;
    }
    if (w.trail.length === 0 || w.trail.length <= layer.drawn) return;
    const g = layer.canvas.getContext('2d')!;
    const R0 = this.store.initialRadius;
    g.save();
    g.scale(LAYER_PX_PER_UNIT, LAYER_PX_PER_UNIT);
    g.translate(R0, R0);
    g.beginPath();
    const from = Math.max(0, layer.drawn - 1);
    g.moveTo(w.trail[from].x, w.trail[from].y);
    for (let i = from + 1; i < w.trail.length; i++) g.lineTo(w.trail[i].x, w.trail[i].y);
    this.strokeTrail(g, w.alive ? this.colorOf(w.playerName) : this.palette.dead);
    g.restore();
    layer.drawn = w.trail.length;
  }

  private strokeTrail(ctx: CanvasRenderingContext2D, color: string) {
    ctx.lineWidth = WORM_RULES.trailRadius * 2;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.strokeStyle = color;
    ctx.stroke();
  }

  private alphaOf(layer: Layer, now: number): number {
    if (layer.deadAt === null) return 1;
    const t = Math.min(1, (now - layer.deadAt) / DEAD_FADE_MS);
    return 1 - (1 - DEAD_MIN_ALPHA) * t;
  }
}
