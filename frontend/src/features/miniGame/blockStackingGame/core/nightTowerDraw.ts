import { StackedBlock } from '@/types/miniGame/blockStackingGame';
import { BLOCK_GAP, BLOCK_HEIGHT, NIGHT_TOWER as C } from '../constants/blockStackingConstants';

/** 야경 빌딩 화면을 그리는 순수 함수 모음. 상태는 useBlockStackingGame 이 들고 매 프레임 넘긴다 */

export const FONT = "'Pretendard Variable', Pretendard, sans-serif";

export type TowerPlayer = {
  name: string;
  floor: number;
  failed: boolean;
  me: boolean;
  color: string;
  /** 층수가 같으면 같은 순위(결과 화면과 같은 기준) */
  rank: number;
  blocks: StackedBlock[];
};

/** 탈락 뒤 스카이라인의 자리 이동·퇴장 애니메이션 상태 */
export type SkylineAnim = {
  disp: Record<string, number>;
  leaving: { player: TowerPlayer; idx: number; at: number }[];
  slot: number;
  count: number;
};

export const createSkylineAnim = (): SkylineAnim => ({ disp: {}, leaving: [], slot: 0, count: 0 });

const lerp = (a: number, b: number, t: number) => a + (b - a) * t;
export const easeOut = (t: number) => 1 - Math.pow(1 - Math.min(1, Math.max(0, t)), 3);
const hash = (n: number) => {
  const x = Math.sin(n * 127.1) * 43758.5453;
  return x - Math.floor(x);
};
const rgb = (hex: string) => [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16));
const mix = (c1: string, c2: string, t: number) => {
  const a = rgb(c1);
  const b = rgb(c2);
  return `rgb(${a.map((v, i) => Math.round(lerp(v, b[i], t))).join(',')})`;
};
const pair = ([day, night]: readonly [string, string], dark: number) => mix(day, night, dark);

const roundRect = (
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number
) => {
  ctx.beginPath();
  ctx.roundRect(x, y, Math.max(0, w), Math.max(0, h), Math.max(0, Math.min(r, w / 2, h / 2)));
};

export const text = (
  ctx: CanvasRenderingContext2D,
  s: string,
  x: number,
  y: number,
  font: string,
  fill: string,
  align: CanvasRenderingContext2D['textAlign'] = 'center',
  base: CanvasRenderingContext2D['textBaseline'] = 'alphabetic'
) => {
  ctx.font = font;
  ctx.fillStyle = fill;
  ctx.textAlign = align;
  ctx.textBaseline = base;
  ctx.fillText(s, x, y);
};

/** 층수에 따른 하늘. phase 0 = 낮, 1 = 노을, 2 = 밤 */
export const skyAt = (floor: number) => {
  const keys = C.sky;
  for (let i = 0; i < keys.length - 1; i++) {
    if (floor <= keys[i + 1].floor) {
      const t = (floor - keys[i].floor) / (keys[i + 1].floor - keys[i].floor);
      return {
        top: mix(keys[i].top, keys[i + 1].top, t),
        bottom: mix(keys[i].bottom, keys[i + 1].bottom, t),
        phase: i + t,
      };
    }
  }
  const last = keys[keys.length - 1];
  return { top: last.top, bottom: last.bottom, phase: keys.length - 1 };
};

export const drawSky = (
  ctx: CanvasRenderingContext2D,
  W: number,
  H: number,
  top: string,
  bottom: string,
  starAlpha: number
) => {
  const grd = ctx.createLinearGradient(0, 0, 0, H);
  grd.addColorStop(0, top);
  grd.addColorStop(1, bottom);
  ctx.fillStyle = grd;
  ctx.fillRect(0, 0, W, H);
  if (starAlpha <= 0) return;
  ctx.fillStyle = `rgba(255,255,255,${starAlpha})`;
  for (let i = 0; i < 40; i++) ctx.fillRect(hash(i) * W, hash(i + 9) * H * 0.65, 1.5, 1.5);
};

/** 멀리 보이는 도시 실루엣. 탑보다 느리게 내려가 원근감을 준다 */
export const drawFarCity = (
  ctx: CanvasRenderingContext2D,
  W: number,
  H: number,
  groundY: number,
  night: boolean
) => {
  const y = H + (groundY - H) * 0.35;
  ctx.fillStyle = night ? C.farCityNight : C.farCityDay;
  for (let i = 0; i * 28 - 6 < W; i++) {
    const h = 40 + hash(i + 3) * 90;
    ctx.fillRect(i * 28 - 6, y - h, 22 + hash(i + 5) * 20, h + H);
  }
};

export const drawTree = (
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  scale: number,
  dark: number,
  seed: number
) => {
  const h = (22 + hash(seed) * 14) * scale;
  ctx.fillStyle = pair(C.trunk, dark);
  ctx.fillRect(x - 2 * scale, y - h * 0.45, 4 * scale, h * 0.45);
  ctx.fillStyle = mix(C.leaves[hash(seed + 1) > 0.5 ? 0 : 1], C.leavesNight, dark);
  (
    [
      [0, -0.62, 0.42],
      [-0.2, -0.45, 0.3],
      [0.2, -0.47, 0.3],
    ] as const
  ).forEach(([dx, dy, r]) => {
    ctx.beginPath();
    ctx.arc(x + dx * h, y + dy * h, h * r, 0, Math.PI * 2);
    ctx.fill();
  });
};

/** 땅(잔디·흙)과 나무. trees 는 [x, 크기] 목록 */
export const drawGround = (
  ctx: CanvasRenderingContext2D,
  W: number,
  H: number,
  y: number,
  dark: number,
  trees: readonly (readonly [number, number])[]
) => {
  ctx.fillStyle = pair(C.soil, dark);
  ctx.fillRect(0, y, W, H - y);
  ctx.fillStyle = pair(C.grass, dark);
  ctx.fillRect(0, y, W, 8);
  ctx.fillStyle = pair(C.pebble, dark);
  for (let i = 0; i < 14; i++) ctx.fillRect(hash(i + 70) * W, y + 16 + hash(i + 90) * 60, 6, 3);
  trees.forEach(([x, scale], i) => drawTree(ctx, x, y + 2, scale, dark, i + 11));
};

/** 창문 달린 빌딩 한 층. lit 는 불 켜진 창 비율, scale 은 스카이라인 축소 비율 */
export const drawFloor = (
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  seed: number,
  lit: number,
  fill: string
) => {
  const scale = h / (BLOCK_HEIGHT - BLOCK_GAP);
  ctx.fillStyle = fill;
  ctx.fillRect(x, y, w, h);
  ctx.fillStyle = 'rgba(255,255,255,0.12)';
  ctx.fillRect(x, y, w, Math.max(1, 2 * scale));
  if (h < 5) return;
  const step = Math.max(4, 14 * scale);
  const n = Math.floor((w - 8 * scale) / step);
  const offset = (w - 8 * scale - n * step) / 2;
  for (let k = 0; k < n; k++) {
    ctx.fillStyle = lit > 0 && hash(seed * 31 + k) > 1 - lit ? C.window : C.windowOff;
    ctx.fillRect(x + 6 * scale + k * step + offset, y + 7 * scale, 8 * scale, 12 * scale);
  }
};

/** 탈락한 빌딩 옥상에 두르는 공사 중단 테이프 */
export const drawTape = (ctx: CanvasRenderingContext2D, x: number, y: number, w: number) => {
  const h = Math.max(4, Math.min(10, w / 8));
  ctx.save();
  ctx.beginPath();
  ctx.rect(x - 4, y, w + 8, h);
  ctx.clip();
  ctx.fillStyle = C.tape;
  ctx.fillRect(x - 4, y, w + 8, h);
  ctx.fillStyle = C.tapeStripe;
  for (let k = -h; k < w + 2 * h; k += h * 1.2) {
    ctx.beginPath();
    ctx.moveTo(x + k, y + h);
    ctx.lineTo(x + k + h * 0.6, y);
    ctx.lineTo(x + k + h * 1.2, y);
    ctx.lineTo(x + k + h * 0.6, y + h);
    ctx.fill();
  }
  ctx.restore();
};

export const drawClock = (
  ctx: CanvasRenderingContext2D,
  cx: number,
  y: number,
  secondsLeft: number,
  warn: boolean
) => {
  ctx.fillStyle = C.panel;
  roundRect(ctx, cx - 44, y, 88, 30, 6);
  ctx.fill();
  const s = Math.ceil(secondsLeft);
  text(
    ctx,
    `0:${String(s).padStart(2, '0')}`,
    cx,
    y + 16,
    `700 18px ${FONT}`,
    warn ? C.warn : C.window,
    'center',
    'middle'
  );
};

export const drawToast = (
  ctx: CanvasRenderingContext2D,
  W: number,
  y: number,
  message: string,
  age: number
) => {
  const life = 2200;
  if (age < 0 || age > life) return;
  ctx.globalAlpha = Math.min(1, (life - age) / 400, age / 150);
  ctx.font = `700 12px ${FONT}`;
  const w = ctx.measureText(message).width + 24;
  ctx.fillStyle = C.me;
  roundRect(ctx, W / 2 - w / 2, y, w, 28, 14);
  ctx.fill();
  text(ctx, message, W / 2, y + 14, `700 12px ${FONT}`, '#fff', 'center', 'middle');
  ctx.globalAlpha = 1;
};

/**
 * 플레이 중 라이벌 높이선. 각자의 현재 층 높이에 점선을 긋고 오른쪽에 이름표를 단다.
 * 화면 위로 벗어난 사람은 위쪽에 "▲ 이름 +차이"로 모은다.
 */
export const drawRivalLines = (
  ctx: CanvasRenderingContext2D,
  W: number,
  H: number,
  rivals: TowerPlayer[],
  myFloor: number,
  yOfFloor: (floor: number) => number,
  topLimit: number
) => {
  const above: TowerPlayer[] = [];
  rivals.forEach((p) => {
    const y = yOfFloor(p.floor) - 1;
    if (y < topLimit) {
      above.push(p);
      return;
    }
    if (y > H) return;
    ctx.strokeStyle = p.failed ? 'rgba(255,255,255,0.3)' : p.color;
    ctx.setLineDash([6, 5]);
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(W, y);
    ctx.stroke();
    ctx.setLineDash([]);
    const label = p.failed ? `${p.name} 중단` : p.name;
    ctx.font = `700 11px ${FONT}`;
    const w = ctx.measureText(label).width + 12;
    ctx.fillStyle = p.failed ? C.out : p.color;
    roundRect(ctx, W - w - 6, y - 9, w, 18, 9);
    ctx.fill();
    text(ctx, label, W - 6 - w / 2, y, `700 11px ${FONT}`, '#fff', 'center', 'middle');
  });
  above
    .sort((a, b) => a.floor - b.floor)
    .forEach((p, k) => {
      const label = `▲ ${p.name} +${p.floor - myFloor}`;
      ctx.font = `700 11px ${FONT}`;
      const w = ctx.measureText(label).width + 14;
      const y = topLimit + k * 22;
      ctx.fillStyle = 'rgba(8,12,24,0.6)';
      roundRect(ctx, W - w - 8, y, w, 18, 9);
      ctx.fill();
      text(ctx, label, W - 8 - w / 2, y + 9, `700 11px ${FONT}`, p.color, 'center', 'middle');
    });
};

const SKYLINE_MAX_SLOT = 72;
const SKYLINE_BASE_SCALE = 0.36;
const LEAVE_MS = 600;

/**
 * 탈락 뒤 스카이라인. 나와 아직 쌓는 사람의 빌딩만 순위대로 세운다(showAll 이면 전원).
 * 칸 폭은 인원수로 나누되 72px 을 넘지 않아 인원이 적으면 양옆이 빈다.
 */
export const drawSkyline = (
  ctx: CanvasRenderingContext2D,
  W: number,
  H: number,
  players: TowerPlayer[],
  showAll: boolean,
  anim: SkylineAnim,
  now: number
) => {
  drawSky(ctx, W, H, C.skylineSky.top, C.skylineSky.bottom, 0.7);
  const ground = H - 70;
  const list = players
    .filter((p) => showAll || p.me || !p.failed)
    .sort((a, b) => a.rank - b.rank || a.name.localeCompare(b.name));
  const targetSlot = Math.min(W / list.length, SKYLINE_MAX_SLOT);
  anim.slot = anim.slot ? lerp(anim.slot, targetSlot, 0.12) : targetSlot;
  anim.count = anim.count ? lerp(anim.count, list.length, 0.12) : list.length;
  const slot = anim.slot;
  const scale = Math.min(SKYLINE_BASE_SCALE, (slot * 0.8) / 150);
  const x0 = (W - slot * anim.count) / 2;
  const tallest = Math.max(...players.map((p) => p.floor), 16);
  const floorH = Math.min(BLOCK_HEIGHT * scale, (ground - 150) / (tallest + 4));

  drawGround(ctx, W, H, ground, 0.9, []);
  for (let k = 0; k <= list.length; k++) {
    drawTree(ctx, x0 + slot * k, ground + 2, (0.55 * scale) / SKYLINE_BASE_SCALE, 0.9, k + 30);
  }
  for (let x = x0 - 36, k = 0; x > 0; x -= 34, k++) {
    drawTree(ctx, x, ground + 2, 0.6, 0.9, k + 50);
    drawTree(ctx, W - x, ground + 2, 0.6, 0.9, k + 60);
  }

  // 목록에서 빠진 사람은 흐려지며 사라진다
  Object.keys(anim.disp).forEach((name) => {
    if (list.some((p) => p.name === name)) return;
    const player = players.find((p) => p.name === name);
    if (player) anim.leaving.push({ player, idx: anim.disp[name], at: now });
    delete anim.disp[name];
  });
  anim.leaving = anim.leaving.filter((l) => now - l.at < LEAVE_MS);

  const small = slot < 60;
  const drawBuilding = (p: TowerPlayer, idx: number, alpha: number) => {
    const cx = x0 + slot * idx + slot / 2;
    const prevAlpha = ctx.globalAlpha;
    ctx.globalAlpha = prevAlpha * alpha;
    let topY = ground;
    p.blocks.forEach((b, j) => {
      const y = ground - (j + 1) * floorH;
      const w = b.width * scale;
      const x = cx + (b.x + b.width / 2 - W / 2) * scale - w / 2;
      drawFloor(ctx, x, y, w, floorH - 1, j, p.failed ? 0.05 : 0.7, j === 0 ? C.base : C.floor);
      topY = y;
    });
    const top = p.blocks[p.blocks.length - 1];
    const topW = top.width * scale;
    const topX = cx + (top.x + top.width / 2 - W / 2) * scale - topW / 2;
    if (p.failed) {
      drawTape(ctx, topX, topY - 4, topW);
    } else if (!showAll) {
      // 아직 쌓는 중: 크레인 줄에 매달린 다음 층
      ctx.strokeStyle = 'rgba(255,255,255,0.45)';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(cx, topY - 6);
      ctx.lineTo(cx, 100);
      ctx.stroke();
      ctx.fillStyle = C.crane;
      ctx.fillRect(cx - topW / 2, topY - 10 - Math.sin(now / 300 + idx) * 3, topW, 4);
    }
    const labelY = topY - 24;
    const label = small ? `${p.floor}F` : `${p.rank}위 ${p.floor}F`;
    const labelW = small ? slot - 4 : 52;
    ctx.fillStyle = p.me ? C.me : C.panel;
    roundRect(ctx, cx - labelW / 2, labelY - 12, labelW, 20, 10);
    ctx.fill();
    text(ctx, label, cx, labelY - 1, `700 ${small ? 9 : 10}px ${FONT}`, '#fff', 'center', 'middle');
    text(
      ctx,
      p.name,
      cx,
      ground + 20,
      `700 ${small ? 10 : 12}px ${FONT}`,
      p.me ? C.meSoft : '#fff'
    );
    const sub = small ? `${p.rank}위` : p.failed ? '중단' : showAll ? '완료' : '공사 중';
    text(ctx, sub, cx, ground + 36, `10px ${FONT}`, p.failed ? C.dead : C.alive);
    ctx.globalAlpha = prevAlpha;
  };

  anim.leaving.forEach((l) => drawBuilding(l.player, l.idx, 1 - (now - l.at) / LEAVE_MS));
  list.forEach((p, i) => {
    const prev = anim.disp[p.name];
    anim.disp[p.name] = prev === undefined ? i : lerp(prev, i, 0.15);
    drawBuilding(p, anim.disp[p.name], 1);
  });

  // 내 높이선: 이 선을 넘는 빌딩이 나를 추월한 사람이다
  const me = players.find((p) => p.me);
  if (me) {
    const y = ground - (me.floor + 1) * floorH;
    ctx.strokeStyle = C.me;
    ctx.lineWidth = 1.5;
    ctx.setLineDash([5, 4]);
    ctx.beginPath();
    ctx.moveTo(6, y);
    ctx.lineTo(W - 6, y);
    ctx.stroke();
    ctx.setLineDash([]);
  }

  const alive = players.filter((p) => !p.me && !p.failed).length;
  const gone = players.length - 1 - alive;
  text(ctx, showAll ? '최종 결과' : '아직 쌓는 사람', 16, 32, `700 14px ${FONT}`, '#fff', 'left');
  text(
    ctx,
    showAll ? `${players.length}명` : `${alive}명 공사 중 · ${gone}명 탈락`,
    16,
    52,
    `11px ${FONT}`,
    'rgba(255,255,255,0.65)',
    'left'
  );
};
