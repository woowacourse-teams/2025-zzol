import {
  BlockStackingGameState,
  BlockStackingRanking,
  CurrentBlock,
  FallingPiece,
  StackedBlock,
} from '@/types/miniGame/blockStackingGame';
import { MutableRefObject, useCallback, useEffect, useLayoutEffect, useRef } from 'react';
import {
  BLOCK_GAP,
  BLOCK_HEIGHT,
  CANVAS_WIDTH,
  GRAVITY,
  INITIAL_BLOCK_WIDTH,
  NIGHT_TOWER,
  OPACITY_DECAY,
} from '@/features/miniGame/blockStackingGame/constants/blockStackingConstants';
import {
  GAME_DURATION,
  PERFECT_THRESHOLD,
  getBlockSpeed,
} from '@/features/miniGame/blockStackingGame/constants/blockStackingBalance';
import {
  FONT,
  TowerPlayer,
  createSkylineAnim,
  drawClock,
  drawFarCity,
  drawFloor,
  drawGround,
  drawRivalLines,
  drawSky,
  drawSkyline,
  drawTape,
  drawToast,
  easeOut,
  skyAt,
  text,
} from '../core/nightTowerDraw';
import { BlockStackingProgressPayload } from './useBlockStackingActions';
import { useBlockStackingSounds } from './useBlockStackingSounds';

type Shake = {
  intensity: number;
  startTime: number;
  duration: number;
};

/** 탈락 뒤 "N층에서 멈춤"을 보여 준 다음 스카이라인으로 넘어가기까지 */
const STOP_HOLD_MS = 500;
const SKYLINE_FADE_MS = 700;
const PLAY_TREES = [
  [22, 1],
  [52, 1.25],
  [268, 1.3],
  [300, 0.95],
] as const;

const updateFallingPieces = (pieces: FallingPiece[], H: number, dt60: number) => {
  return pieces
    .map((p) => ({
      ...p,
      y: p.y + p.vy * dt60,
      vy: p.vy + GRAVITY * dt60,
      opacity: Math.max(0, p.opacity - OPACITY_DECAY * dt60),
    }))
    .filter((p) => p.opacity > 0 && p.y < H + 100);
};

/** cur 를 in-place 로 변이한다 (RAF 루프 alloc 회피). */
const updateCurrentBlock = (cur: CurrentBlock, speed: number, dt60: number, W: number) => {
  let nx = cur.x + speed * dt60 * cur.direction;
  let nd = cur.direction;
  if (nx <= 0) {
    nx = 0;
    nd = 1;
  } else if (nx + cur.width >= W) {
    nx = W - cur.width;
    nd = -1;
  }
  cur.x = nx;
  cur.direction = nd;
};

const toFallingPiece = (x: number, y: number, width: number): FallingPiece => ({
  x,
  y,
  width,
  vy: 1,
  opacity: 1,
  color: NIGHT_TOWER.crane,
});

const createOverhangPieces = ({
  blockX,
  blockWidth,
  leftEdge,
  rightEdge,
  y,
}: {
  blockX: number;
  blockWidth: number;
  leftEdge: number;
  rightEdge: number;
  y: number;
}): FallingPiece[] => {
  const pieces: FallingPiece[] = [];
  if (blockX < leftEdge) {
    pieces.push(toFallingPiece(blockX, y, leftEdge - blockX));
  }
  if (blockX + blockWidth > rightEdge) {
    pieces.push(toFallingPiece(rightEdge, y, blockX + blockWidth - rightEdge));
  }
  return pieces;
};

/**
 * 블록 쌓기 게임의 핵심 로직을 담당하는 커스텀 훅
 * 캔버스 렌더링, 물리 연산, 게임 상태 관리, 타이머 동기화 등을 수행합니다.
 *
 * @param canvasRef - 렌더링될 캔버스 엘리먼트의 Ref
 * @param gameState - 현재 게임의 진행 상태 (PREPARE, PLAYING, DONE 등)
 * @param isLocalGameOver - 현재 플레이어의 탈락 여부
 * @param endTimeEpochMs - 서버에서 전송된 게임 종료 시각 (Sync용)
 * @param options - 사운드 재생, 게임 오버 콜백, 진행 상황 보고, 다른 참가자 정보 등을 포함한 객체
 */
export const useBlockStackingGame = (
  canvasRef: MutableRefObject<HTMLCanvasElement | null>,
  gameState: BlockStackingGameState,
  isLocalGameOver: boolean,
  endTimeEpochMs: number | null,
  options: {
    setLocalGameOver: () => void;
    sounds: ReturnType<typeof useBlockStackingSounds>;
    onBlockPlaced: (payload: BlockStackingProgressPayload) => void;
    onFail: () => void;
    myName: string;
    rankings: BlockStackingRanking[];
    towers: Record<string, StackedBlock[]>;
    colorOf: (name: string) => string;
  }
) => {
  const { setLocalGameOver, sounds, onBlockPlaced, onFail, myName, rankings, towers, colorOf } =
    options;

  // --- 1. Game State & Refs (내부 상태 관리) ---
  const stackRef = useRef<StackedBlock[]>([]);
  const currentBlockRef = useRef<CurrentBlock>({
    x: 0,
    width: INITIAL_BLOCK_WIDTH,
    direction: 1,
  });
  const fallingPiecesRef = useRef<FallingPiece[]>([]);
  const shakeRef = useRef<Shake>({ intensity: 0, startTime: 0, duration: 0 });
  const scoreRef = useRef(0);
  const cameraYRef = useRef(0);
  const timeLeftRef = useRef(GAME_DURATION);
  /** 내가 멈춘 시각. 0 이면 아직 쌓는 중 */
  const stoppedAtRef = useRef(0);
  const failedRef = useRef(false);
  const perfectAtRef = useRef(-Infinity);
  const skylineRef = useRef(createSkylineAnim());
  /** 추월 알림용: 직전 프레임의 내 순위와 각자의 층 */
  const myRankRef = useRef(0);
  const floorsRef = useRef<Record<string, number>>({});
  const toastRef = useRef({ message: '', at: -Infinity });

  // --- 2. Closure Avoidance (클로저 문제 해결) ---
  // 아래 Ref들은 handleTap이나 루프 내부에서 최신 Props/Callbacks에 접근할 수 있게 합니다.
  const gameStateRef = useRef(gameState);
  const soundsRef = useRef(sounds);
  const setLocalGameOverRef = useRef(setLocalGameOver);
  const onBlockPlacedRef = useRef(onBlockPlaced);
  const onFailRef = useRef(onFail);
  const isLocalGameOverRef = useRef(isLocalGameOver);
  const myNameRef = useRef(myName);
  const rankingsRef = useRef(rankings);
  const towersRef = useRef(towers);
  const colorOfRef = useRef(colorOf);
  // rAF 그리기 루프가 페인트 전에 최신값을 읽어야 하므로(원래 렌더 중 동기 갱신) useLayoutEffect 로 커밋 시 동기 반영한다.
  useLayoutEffect(() => {
    gameStateRef.current = gameState;
    soundsRef.current = sounds;
    setLocalGameOverRef.current = setLocalGameOver;
    onBlockPlacedRef.current = onBlockPlaced;
    onFailRef.current = onFail;
    isLocalGameOverRef.current = isLocalGameOver;
    myNameRef.current = myName;
    rankingsRef.current = rankings;
    towersRef.current = towers;
    colorOfRef.current = colorOf;
  });

  // --- 3. Game Actions (주요 액션 함수) ---

  /**
   * 화면 터치(Tap) 시 실행되는 블록 배치 로직
   */
  const handleTap = useCallback(() => {
    // 게임 중이 아니거나 이미 탈락한 경우 무시
    if (gameStateRef.current !== 'PLAYING' || isLocalGameOverRef.current) return;

    soundsRef.current.ensureAudioContext();

    const stack = stackRef.current;
    if (stack.length === 0) return;

    const topBlock = stack[stack.length - 1];
    if (!topBlock) return;
    const cur = currentBlockRef.current;

    // 겹치는 영역 계산
    const leftEdge = Math.max(cur.x, topBlock.x);
    const rightEdge = Math.min(cur.x + cur.width, topBlock.x + topBlock.width);
    const overlap = Math.round(rightEdge - leftEdge);

    // [Case A] 완전히 빗나간 경우: 게임 오버 처리
    if (overlap <= 0) {
      fallingPiecesRef.current.push(toFallingPiece(cur.x, cameraYRef.current, cur.width));

      shakeRef.current = { intensity: 12, startTime: performance.now(), duration: 500 };
      stoppedAtRef.current = performance.now();
      failedRef.current = true;
      soundsRef.current.playGameOver();
      setLocalGameOverRef.current();
      onFailRef.current();
      return;
    }

    // [Case B] 블록 일부가 겹친 경우: 다음 단계 진행
    const isPerfect =
      Math.abs(cur.x - topBlock.x) < PERFECT_THRESHOLD &&
      Math.abs(cur.x + cur.width - (topBlock.x + topBlock.width)) < PERFECT_THRESHOLD;

    // 퍼펙트가 아닐 경우 잘려나가는 조각들 생성
    if (!isPerfect) {
      const newPieces = createOverhangPieces({
        blockX: cur.x,
        blockWidth: cur.width,
        leftEdge,
        rightEdge,
        y: cameraYRef.current,
      });
      fallingPiecesRef.current = [...fallingPiecesRef.current, ...newPieces];
    }

    // 새 블록 스택에 추가
    const newBlock: StackedBlock = {
      x: isPerfect ? topBlock.x : leftEdge,
      width: isPerfect ? topBlock.width : overlap,
    };
    stackRef.current = [...stack, newBlock];

    // 점수 업데이트
    const prevScore = scoreRef.current;
    const newScore = prevScore + 1;
    scoreRef.current = newScore;

    // 다음 블록 준비 (위치와 크기 고정, 방향은 유지)
    currentBlockRef.current.x = newBlock.x;
    currentBlockRef.current.width = newBlock.width;

    // 서버로 현재 진행 상황 보고 (실시간 랭킹용)
    onBlockPlacedRef.current({
      floor: newScore,
      movingBlockX: cur.x,
      stackTopX: topBlock.x,
      stackTopWidth: topBlock.width,
    });

    // 시각/청각 피드백
    if (isPerfect) {
      shakeRef.current = { intensity: 6, startTime: performance.now(), duration: 300 };
      perfectAtRef.current = performance.now();
      soundsRef.current.playPerfect();
    } else {
      shakeRef.current = { intensity: 3, startTime: performance.now(), duration: 200 };
      soundsRef.current.playLand();
    }

    // 난이도 상승 알림
    if (getBlockSpeed(prevScore) !== getBlockSpeed(newScore)) {
      soundsRef.current.playSpeedUp();
    }
  }, []);

  // --- 4. Effects (생명주기 및 동기화) ---

  /**
   * 서버 종료 시각(endTimeEpochMs) 기반 타이머 동기화 Effect (60 FPS 기반)
   */
  useEffect(() => {
    if (gameState !== 'PLAYING' || endTimeEpochMs == null) return;

    let rafId: number;
    const computeRemaining = () => Math.max(0, (endTimeEpochMs - Date.now()) / 1000);

    const updateTimer = () => {
      const remaining = computeRemaining();
      timeLeftRef.current = remaining;

      if (remaining <= 0) {
        if (!stoppedAtRef.current) stoppedAtRef.current = performance.now();
        setLocalGameOverRef.current();
        return;
      }

      rafId = requestAnimationFrame(updateTimer);
    };

    rafId = requestAnimationFrame(updateTimer);
    return () => cancelAnimationFrame(rafId);
  }, [gameState, endTimeEpochMs]);

  /**
   * 메인 게임 루프 및 캔버스 초기화 Effect
   */
  useEffect(() => {
    if (gameState !== 'PLAYING') return;

    // 게임 시작 시 초기값 설정
    scoreRef.current = 0;

    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // 가상 좌표 시스템 설정 (CANVAS_WIDTH = 320 기준)
    const scale = canvas.width / CANVAS_WIDTH;
    const virtualWidth = CANVAS_WIDTH;
    const virtualHeight = canvas.height / scale;

    // 초기 상태 셋팅 (가상 좌표 기준)
    const initialX = (virtualWidth - INITIAL_BLOCK_WIDTH) / 2;
    stackRef.current = [{ x: initialX, width: INITIAL_BLOCK_WIDTH }];
    currentBlockRef.current = { x: initialX, width: INITIAL_BLOCK_WIDTH, direction: 1 };
    fallingPiecesRef.current = [];
    shakeRef.current = { intensity: 0, startTime: 0, duration: 0 };
    cameraYRef.current = virtualHeight - 2 * BLOCK_HEIGHT;
    timeLeftRef.current = GAME_DURATION;
    stoppedAtRef.current = 0;
    failedRef.current = false;
    perfectAtRef.current = -Infinity;
    skylineRef.current = createSkylineAnim();
    myRankRef.current = 0;
    floorsRef.current = {};
    toastRef.current = { message: '', at: -Infinity };

    let prevTime = 0;
    let rafId: number;

    /** 나와 다른 참가자를 한 목록으로. 순위는 층수가 같으면 같게 매긴다(결과 화면 기준) */
    const collectPlayers = (): TowerPlayer[] => {
      const me = myNameRef.current;
      const players: Omit<TowerPlayer, 'rank'>[] = rankingsRef.current
        .filter((r) => r.name !== me)
        .map((r) => ({
          name: r.name,
          floor: r.floor,
          failed: r.failed,
          me: false,
          color: colorOfRef.current(r.name),
          blocks: towersRef.current[r.name] ?? [stackRef.current[0]],
        }));
      players.push({
        name: me,
        floor: scoreRef.current,
        failed: failedRef.current,
        me: true,
        color: NIGHT_TOWER.me,
        blocks: stackRef.current,
      });
      return players.map((p) => ({
        ...p,
        rank: 1 + players.filter((q) => q.floor > p.floor).length,
      }));
    };

    /** 탈락한 뒤 순위가 밀리면 누가 나를 넘었는지 알린다 */
    const trackOvertake = (players: TowerPlayer[], now: number) => {
      const me = players.find((p) => p.me);
      if (!me) return;
      const prevRank = myRankRef.current;
      if (stoppedAtRef.current && prevRank && me.rank > prevRank) {
        const passer = players.find(
          (p) => !p.me && p.floor > me.floor && (floorsRef.current[p.name] ?? 0) <= me.floor
        );
        if (passer) {
          toastRef.current = {
            message: `${passer.name}님이 나를 넘었어요 · ${prevRank}위 → ${me.rank}위`,
            at: now,
          };
        }
      }
      myRankRef.current = me.rank;
      players.forEach((p) => {
        floorsRef.current[p.name] = p.floor;
      });
    };

    const drawPlayScene = (
      W: number,
      H: number,
      movingBlockY: number,
      players: TowerPlayer[],
      now: number
    ) => {
      const stack = stackRef.current;
      const score = scoreRef.current;
      const stopped = stoppedAtRef.current > 0;
      const sky = skyAt(score);
      drawSky(ctx, W, H, sky.top, sky.bottom, (sky.phase - 1) * 0.8);

      // 화면 흔들림 효과 연산
      const shake = shakeRef.current;
      const shakeProgress =
        shake.duration > 0 ? Math.max(0, 1 - (now - shake.startTime) / shake.duration) : 0;
      const sx = (Math.random() * 2 - 1) * shake.intensity * shakeProgress;
      const sy = (Math.random() * 2 - 1) * shake.intensity * shakeProgress;

      ctx.save();
      ctx.translate(sx, sy);

      const blockY = (i: number) => movingBlockY + (stack.length - i) * BLOCK_HEIGHT;
      const groundY = blockY(0) + BLOCK_HEIGHT - BLOCK_GAP;
      drawFarCity(ctx, W, H, groundY, sky.phase > 1);
      if (groundY < H + 60) {
        drawGround(ctx, W, H, groundY, Math.min(1, sky.phase / 1.6), PLAY_TREES);
      }

      // 쌓여있는 빌딩 층
      const lit = sky.phase > 0.6 ? 0.65 : 0.2;
      stack.forEach((block, i) => {
        const y = blockY(i);
        if (y > H + BLOCK_HEIGHT) return;
        const fill = i === 0 ? NIGHT_TOWER.base : NIGHT_TOWER.floor;
        drawFloor(
          ctx,
          block.x,
          y,
          block.width,
          BLOCK_HEIGHT - BLOCK_GAP,
          i,
          stopped ? 0.08 : lit,
          fill
        );
      });

      const top = stack[stack.length - 1];
      if (stopped) {
        drawTape(ctx, top.x, blockY(stack.length - 1) - 5, top.width);
      } else {
        // 크레인 줄에 매달린 다음 층
        const cur = currentBlockRef.current;
        const cx = cur.x + cur.width / 2;
        ctx.strokeStyle = 'rgba(255,255,255,0.55)';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(cx - 14, movingBlockY);
        ctx.lineTo(cx, movingBlockY - 16);
        ctx.lineTo(cx + 14, movingBlockY);
        ctx.moveTo(cx, movingBlockY - 16);
        ctx.lineTo(cx, 0);
        ctx.stroke();
        drawFloor(
          ctx,
          cur.x,
          movingBlockY,
          cur.width,
          BLOCK_HEIGHT - BLOCK_GAP,
          stack.length,
          lit,
          NIGHT_TOWER.crane
        );
      }

      fallingPiecesRef.current.forEach((p) => {
        ctx.globalAlpha = p.opacity;
        ctx.fillStyle = p.color;
        ctx.fillRect(p.x, p.y, p.width, BLOCK_HEIGHT - BLOCK_GAP);
      });
      ctx.globalAlpha = 1;

      const perfectAge = (now - perfectAtRef.current) / 700;
      if (perfectAge < 1) {
        ctx.globalAlpha = 1 - perfectAge;
        const y = blockY(stack.length - 1) - 14 - perfectAge * 22;
        text(ctx, 'PERFECT', W / 2, y, `800 18px ${FONT}`, NIGHT_TOWER.window);
        ctx.globalAlpha = 1;
      }
      ctx.restore(); // shake translate restore

      drawRivalLines(
        ctx,
        W,
        H,
        players.filter((p) => !p.me),
        score,
        blockY,
        64
      );

      // HUD: 시계, 내 층수와 순위 (우상단은 소리 버튼 자리)
      const me = players.find((p) => p.me);
      drawClock(ctx, W / 2, 12, timeLeftRef.current, timeLeftRef.current < 5);
      text(ctx, `${score}층`, 14, 36, `800 24px ${FONT}`, '#fff', 'left');
      text(
        ctx,
        `${me?.rank ?? 1}위 / ${players.length}명`,
        14,
        54,
        `700 12px ${FONT}`,
        'rgba(255,255,255,0.8)',
        'left'
      );
    };

    /**
     * 프레임 드로우 함수 (Main Loop)
     */
    const draw = (time: number) => {
      // 탭 전환 복귀 시 prevTime 리셋 — 장시간 중단 후 첫 프레임을 정상 처리
      if (document.hidden) {
        prevTime = 0;
        rafId = requestAnimationFrame(draw);
        return;
      }
      // 델타 타임 계산 후 60fps 기준으로 정규화
      // 첫 프레임은 1프레임으로 처리, 최대 3프레임으로 제한
      const deltaMs = prevTime > 0 ? time - prevTime : 1000 / 60;
      prevTime = time;
      const dt60 = Math.min((deltaMs / 1000) * 60, 3);
      // 매 프레임 스케일 재계산 (창 크기 조절 대응)
      const currentScale = canvas.width / CANVAS_WIDTH;
      const W = CANVAS_WIDTH;
      const H = canvas.height / currentScale;

      ctx.clearRect(0, 0, canvas.width, canvas.height);

      // 가상 좌표계로 변환하여 그리기
      ctx.save();
      ctx.scale(currentScale, currentScale);

      const stack = stackRef.current;
      const isGameOver = isLocalGameOverRef.current;

      // [Update Logic] 게임 중일 때만 블록 이동
      if (!isGameOver) {
        updateCurrentBlock(currentBlockRef.current, getBlockSpeed(scoreRef.current), dt60, W);
      }

      // [Camera Logic] 카메라 팔로우 부드럽게 이동
      // exponential decay: 프레임 독립적 lerp (단순 * dt60은 고FPS/탭복귀 시 오버슈팅 발생)
      const targetCameraY = Math.max(H / 2, H - (stack.length + 1) * BLOCK_HEIGHT);
      cameraYRef.current += (targetCameraY - cameraYRef.current) * (1 - Math.pow(0.9, dt60));
      const movingBlockY = cameraYRef.current;

      // 낙하 중인 조각들 업데이트
      fallingPiecesRef.current = updateFallingPieces(fallingPiecesRef.current, H, dt60);

      const players = collectPlayers();
      trackOvertake(players, time);

      drawPlayScene(W, H, movingBlockY, players, time);

      // 탈락 뒤: 잠깐 "N층에서 멈춤"을 보여 주고 스카이라인으로 넘어간다
      const stoppedAt = stoppedAtRef.current;
      if (stoppedAt) {
        const t = (time - stoppedAt - STOP_HOLD_MS) / SKYLINE_FADE_MS;
        if (t <= 0) {
          ctx.fillStyle = 'rgba(0,0,0,0.3)';
          ctx.fillRect(0, 0, W, H);
          text(
            ctx,
            `${scoreRef.current}층에서 멈춤`,
            W / 2,
            H * 0.3,
            `800 26px ${FONT}`,
            NIGHT_TOWER.warn
          );
        } else {
          // 시간이 다 돼서 멈췄으면 결과 비교를 위해 전원을 세운다
          const showAll = !failedRef.current;
          ctx.globalAlpha = easeOut(t);
          drawSkyline(ctx, W, H, players, showAll, skylineRef.current, time);
          drawClock(ctx, W / 2, 12, timeLeftRef.current, timeLeftRef.current < 5);
          drawToast(ctx, W, 64, toastRef.current.message, time - toastRef.current.at);
          ctx.globalAlpha = 1;
        }
      }

      ctx.restore(); // scale restore

      // 서버 대기 모드(DONE)가 되기 전까지 애니메이션 루프 유지
      if (gameStateRef.current === 'PLAYING') {
        rafId = requestAnimationFrame(draw);
      }
    };

    rafId = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(rafId);
  }, [gameState, canvasRef]);

  return { handleTap };
};

// 외부에서 CANVAS_WIDTH를 참조할 수 있도록 재-export
export { CANVAS_WIDTH };
