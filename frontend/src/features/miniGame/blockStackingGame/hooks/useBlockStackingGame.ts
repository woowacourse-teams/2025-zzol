import {
  BlockStackingGameState,
  CurrentBlock,
  FallingPiece,
  StackedBlock,
} from '@/types/miniGame/blockStackingGame';
import { MutableRefObject, useCallback, useEffect, useRef, useState } from 'react';
import {
  BLOCK_COLORS,
  BLOCK_GAP,
  BLOCK_HEIGHT,
  CANVAS_WIDTH,
  GRAVITY,
  INITIAL_BLOCK_WIDTH,
  OPACITY_DECAY,
  PERFECT_THRESHOLD,
  GAME_DURATION,
  getBlockSpeed,
} from '../constants/blockStackingConstants';
import { BlockStackingProgressPayload } from './useBlockStackingActions';
import { useBlockStackingSounds } from './useBlockStackingSounds';

type Shake = {
  intensity: number;
  startTime: number;
  duration: number;
};

const lerpInt = (a: number, b: number, t: number) => Math.round(a + (b - a) * t);

// --- Drawing Helpers ---

const drawBackground = (ctx: CanvasRenderingContext2D, W: number, H: number, score: number) => {
  const darken = Math.min(score / 40, 1);
  ctx.fillStyle = `rgb(${lerpInt(135, 15, darken)}, ${lerpInt(190, 15, darken)}, ${lerpInt(240, 60, darken)})`;
  ctx.fillRect(0, 0, W, H);
};

const drawStackedBlocks = (
  ctx: CanvasRenderingContext2D,
  stack: StackedBlock[],
  movingBlockY: number,
  H: number
) => {
  stack.forEach((block, i) => {
    const dist = stack.length - i;
    const y = movingBlockY + dist * BLOCK_HEIGHT;
    if (y > H + BLOCK_HEIGHT) return;

    ctx.shadowColor = 'rgba(0,0,0,0.25)';
    ctx.shadowBlur = 4;
    ctx.shadowOffsetY = 2;
    ctx.fillStyle = BLOCK_COLORS[i % BLOCK_COLORS.length];
    ctx.fillRect(block.x, y, block.width, BLOCK_HEIGHT - BLOCK_GAP);

    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    ctx.fillStyle = 'rgba(255,255,255,0.2)';
    ctx.fillRect(block.x, y, block.width, 3);
  });
};

const drawCurrentBlock = (
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  width: number,
  color: string
) => {
  ctx.shadowColor = 'rgba(0,0,0,0.4)';
  ctx.shadowBlur = 8;
  ctx.shadowOffsetY = 4;
  ctx.fillStyle = color;
  ctx.fillRect(x, y, width, BLOCK_HEIGHT - BLOCK_GAP);

  ctx.shadowColor = 'transparent';
  ctx.shadowBlur = 0;
  ctx.fillStyle = 'rgba(255,255,255,0.3)';
  ctx.fillRect(x, y, width, 3);
};

const drawFallingPieces = (ctx: CanvasRenderingContext2D, pieces: FallingPiece[]) => {
  pieces.forEach((p) => {
    ctx.globalAlpha = p.opacity;
    ctx.fillStyle = p.color;
    ctx.fillRect(p.x, p.y, p.width, BLOCK_HEIGHT - BLOCK_GAP);
  });
  ctx.globalAlpha = 1;
};

const updateFallingPieces = (pieces: FallingPiece[], H: number) => {
  return pieces
    .map((p) => ({
      ...p,
      y: p.y + p.vy,
      vy: p.vy + GRAVITY,
      opacity: Math.max(0, p.opacity - OPACITY_DECAY),
    }))
    .filter((p) => p.opacity > 0 && p.y < H + 100);
};

/**
 * 블록 쌓기 게임의 핵심 로직을 담당하는 커스텀 훅
 * 캔버스 렌더링, 물리 연산, 게임 상태 관리, 타이머 동기화 등을 수행합니다.
 *
 * @param canvasRef - 렌더링될 캔버스 엘리먼트의 Ref
 * @param gameState - 현재 게임의 진행 상태 (PREPARE, PLAYING, DONE 등)
 * @param isLocalGameOver - 현재 플레이어의 탈락 여부
 * @param endTimeEpochMs - 서버에서 전송된 게임 종료 시각 (Sync용)
 * @param options - 사운드 재생, 게임 오버 콜백, 진행 상황 보고 등을 포함한 객체
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
  }
) => {
  const { setLocalGameOver, sounds, onBlockPlaced } = options;

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

  // 화면에 점수와 시간을 표시하기 위한 State (렌더링 트리거용)
  const [score, setScore] = useState(0);
  const [timeLeft, setTimeLeft] = useState(GAME_DURATION);

  // --- 2. Closure Avoidance (클로저 문제 해결) ---
  // 아래 Ref들은 handleTap이나 루프 내부에서 최신 Props/Callbacks에 접근할 수 있게 합니다.
  const soundsRef = useRef(sounds);
  soundsRef.current = sounds;
  const setLocalGameOverRef = useRef(setLocalGameOver);
  setLocalGameOverRef.current = setLocalGameOver;
  const onBlockPlacedRef = useRef(onBlockPlaced);
  onBlockPlacedRef.current = onBlockPlaced;
  const isLocalGameOverRef = useRef(isLocalGameOver);
  isLocalGameOverRef.current = isLocalGameOver;

  // --- 3. Game Actions (주요 액션 함수) ---

  /**
   * 화면 터치(Tap) 시 실행되는 블록 배치 로직
   */
  const handleTap = useCallback(() => {
    // 게임 중이 아니거나 이미 탈락한 경우 무시
    if (gameState !== 'PLAYING' || isLocalGameOverRef.current) return;

    soundsRef.current.ensureAudioContext();

    const stack = stackRef.current;
    if (stack.length === 0) return;

    const topBlock = stack[stack.length - 1];
    if (!topBlock) return;
    const cur = currentBlockRef.current;
    const currentColor = BLOCK_COLORS[stack.length % BLOCK_COLORS.length];

    // 겹치는 영역 계산
    const leftEdge = Math.max(cur.x, topBlock.x);
    const rightEdge = Math.min(cur.x + cur.width, topBlock.x + topBlock.width);
    const overlap = Math.round(rightEdge - leftEdge);

    // [Case A] 완전히 빗나간 경우: 게임 오버 처리
    if (overlap <= 0) {
      fallingPiecesRef.current.push({
        x: cur.x,
        y: cameraYRef.current,
        width: cur.width,
        vy: 1,
        opacity: 1,
        color: currentColor,
      });

      shakeRef.current = { intensity: 12, startTime: performance.now(), duration: 500 };
      soundsRef.current.playGameOver();
      setLocalGameOverRef.current();
      return;
    }

    // [Case B] 블록 일부가 겹친 경우: 다음 단계 진행
    const isPerfect =
      Math.abs(cur.x - topBlock.x) < PERFECT_THRESHOLD &&
      Math.abs(cur.x + cur.width - (topBlock.x + topBlock.width)) < PERFECT_THRESHOLD;

    // 퍼펙트가 아닐 경우 잘려나가는 조각들 생성
    if (!isPerfect) {
      const newPieces: FallingPiece[] = [];
      const currentY = cameraYRef.current;
      if (cur.x < leftEdge) {
        newPieces.push({
          x: cur.x,
          y: currentY,
          width: leftEdge - cur.x,
          vy: 1,
          opacity: 1,
          color: currentColor,
        });
      }
      if (cur.x + cur.width > rightEdge) {
        newPieces.push({
          x: rightEdge,
          y: currentY,
          width: cur.x + cur.width - rightEdge,
          vy: 1,
          opacity: 1,
          color: currentColor,
        });
      }
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
    setScore(newScore);

    // 다음 블록 준비 (위치와 크기 고정)
    currentBlockRef.current.x = newBlock.x;
    currentBlockRef.current.width = newBlock.width;
    currentBlockRef.current.direction = 1;

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
      soundsRef.current.playPerfect();
    } else {
      shakeRef.current = { intensity: 3, startTime: performance.now(), duration: 200 };
      soundsRef.current.playLand();
    }

    // 난이도 상승 알림
    if (getBlockSpeed(prevScore) !== getBlockSpeed(newScore)) {
      soundsRef.current.playSpeedUp();
    }
  }, [gameState]);

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
      setTimeLeft(remaining);

      if (remaining <= 0) {
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
    setScore(0);

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

    let rafId: number;

    /**
     * 프레임 드로우 함수 (Main Loop)
     */
    const draw = (time: number) => {
      // 매 프레임 스케일 재계산 (창 크기 조절 대응)
      const currentScale = canvas.width / CANVAS_WIDTH;
      const W = CANVAS_WIDTH;
      const H = canvas.height / currentScale;

      ctx.clearRect(0, 0, canvas.width, canvas.height);
      
      // 가상 좌표계로 변환하여 그리기
      ctx.save();
      ctx.scale(currentScale, currentScale);

      drawBackground(ctx, W, H, scoreRef.current);

      // 화면 흔들림 효과 연산
      const shake = shakeRef.current;
      const shakeProgress =
        shake.duration > 0 ? Math.max(0, 1 - (time - shake.startTime) / shake.duration) : 0;
      const sx = (Math.random() * 2 - 1) * shake.intensity * shakeProgress;
      const sy = (Math.random() * 2 - 1) * shake.intensity * shakeProgress;

      ctx.save();
      ctx.translate(sx, sy);

      const stack = stackRef.current;
      const isGameOver = isLocalGameOverRef.current;

      // [Update Logic] 게임 중일 때만 블록 이동
      if (!isGameOver) {
        const cur = currentBlockRef.current;
        const speed = getBlockSpeed(scoreRef.current);
        let nx = cur.x + speed * cur.direction;
        let nd = cur.direction;
        if (nx <= 0) {
          nx = 0;
          nd = 1;
        } else if (nx + cur.width >= W) {
          nx = W - cur.width;
          nd = -1;
        }
        currentBlockRef.current.x = nx;
        currentBlockRef.current.direction = nd;
      }

      // [Camera Logic] 카메라 팔로우 부드럽게 이동
      const targetCameraY = Math.max(H / 2, H - (stack.length + 1) * BLOCK_HEIGHT);
      cameraYRef.current += (targetCameraY - cameraYRef.current) * 0.1;
      const movingBlockY = cameraYRef.current;

      // [Drawing Logic] 쌓여있는 블록들 렌더링
      drawStackedBlocks(ctx, stack, movingBlockY, H);

      // 현재 움직이는 블록 렌더링
      if (!isGameOver) {
        const cur = currentBlockRef.current;
        const color = BLOCK_COLORS[stack.length % BLOCK_COLORS.length];
        drawCurrentBlock(ctx, cur.x, movingBlockY, cur.width, color);
      }

      // 낙하 중인 조각들 업데이트 및 렌더링
      fallingPiecesRef.current = updateFallingPieces(fallingPiecesRef.current, H);
      drawFallingPieces(ctx, fallingPiecesRef.current);

      // 스코어 텍스트 표시
      ctx.fillStyle = 'rgba(255,255,255,0.9)';
      ctx.font = `bold 28px 'Pretendard Variable', Pretendard, sans-serif`;
      ctx.textAlign = 'center';
      ctx.fillText(`${scoreRef.current}층`, W / 2, 60);

      ctx.restore(); // shake translate restore
      ctx.restore(); // scale restore

      // 서버 대기 모드(DONE)가 되기 전까지 애니메이션 루프 유지
      if (gameState === 'PLAYING') {
        rafId = requestAnimationFrame(draw);
      }
    };

    rafId = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(rafId);
  }, [gameState, canvasRef]);

  return { score, timeLeft, handleTap };
};

// 외부에서 CANVAS_WIDTH를 참조할 수 있도록 재-export
export { CANVAS_WIDTH };
