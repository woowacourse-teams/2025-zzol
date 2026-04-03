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

  const stackRef = useRef<StackedBlock[]>([]);
  const currentBlockRef = useRef<CurrentBlock>({
    x: 0,
    width: INITIAL_BLOCK_WIDTH,
    direction: 1,
  });
  const fallingPiecesRef = useRef<FallingPiece[]>([]);
  const shakeRef = useRef<Shake>({ intensity: 0, startTime: 0, duration: 0 });
  const scoreRef = useRef(0);

  const [score, setScore] = useState(0);
  const [timeLeft, setTimeLeft] = useState(GAME_DURATION);

  // Refs로 최신 콜백 유지 (handleTap 재생성 최소화)
  const soundsRef = useRef(sounds);
  soundsRef.current = sounds;
  const setLocalGameOverRef = useRef(setLocalGameOver);
  setLocalGameOverRef.current = setLocalGameOver;
  const onBlockPlacedRef = useRef(onBlockPlaced);
  onBlockPlacedRef.current = onBlockPlaced;
  const isLocalGameOverRef = useRef(isLocalGameOver);
  isLocalGameOverRef.current = isLocalGameOver;

  const cameraYRef = useRef(0);

  const handleTap = useCallback(() => {
    if (gameState !== 'PLAYING' || isLocalGameOverRef.current) return;

    soundsRef.current.ensureAudioContext();

    const stack = stackRef.current;
    const topBlock = stack[stack.length - 1];
    const cur = currentBlockRef.current;

    const leftEdge = Math.max(cur.x, topBlock.x);
    const rightEdge = Math.min(cur.x + cur.width, topBlock.x + topBlock.width);
    const overlap = Math.round(rightEdge - leftEdge);

    if (overlap <= 0) {
      shakeRef.current = { intensity: 12, startTime: performance.now(), duration: 500 };
      soundsRef.current.playGameOver();
      setLocalGameOverRef.current();
      return;
    }

    const isPerfect =
      Math.abs(cur.x - topBlock.x) < PERFECT_THRESHOLD &&
      Math.abs(cur.x + cur.width - (topBlock.x + topBlock.width)) < PERFECT_THRESHOLD;

    if (!isPerfect) {
      const newPieces: FallingPiece[] = [];
      const currentY = cameraYRef.current;
      if (cur.x < leftEdge) {
        newPieces.push({ x: cur.x, y: currentY, width: leftEdge - cur.x, vy: 1, opacity: 1 });
      }
      if (cur.x + cur.width > rightEdge) {
        newPieces.push({
          x: rightEdge,
          y: currentY,
          width: cur.x + cur.width - rightEdge,
          vy: 1,
          opacity: 1,
        });
      }
      fallingPiecesRef.current = [...fallingPiecesRef.current, ...newPieces];
    }

    const newBlock: StackedBlock = {
      x: isPerfect ? topBlock.x : leftEdge,
      width: isPerfect ? topBlock.width : overlap,
    };
    stackRef.current = [...stack, newBlock];

    const prevScore = scoreRef.current;
    const newScore = prevScore + 1;
    scoreRef.current = newScore;
    setScore(newScore);

    currentBlockRef.current.x = newBlock.x;
    currentBlockRef.current.width = newBlock.width;
    currentBlockRef.current.direction = 1;

    onBlockPlacedRef.current({
      floor: newScore,
      tapX: cur.x,
      movingBlockX: cur.x,
      stackTopX: topBlock.x,
      stackTopWidth: topBlock.width,
    });

    if (isPerfect) {
      shakeRef.current = { intensity: 6, startTime: performance.now(), duration: 300 };
      soundsRef.current.playPerfect();
    } else {
      shakeRef.current = { intensity: 3, startTime: performance.now(), duration: 200 };
      soundsRef.current.playLand();
    }

    if (getBlockSpeed(prevScore) !== getBlockSpeed(newScore)) {
      soundsRef.current.playSpeedUp();
    }
  }, [gameState]);

  // 타이머 — endTimeEpochMs 기준으로 동기화. 로컬 만료 시 루프만 중단.
  useEffect(() => {
    if (gameState !== 'PLAYING' || endTimeEpochMs == null) return;

    const computeRemaining = () => Math.max(0, Math.ceil((endTimeEpochMs - Date.now()) / 1000));

    setTimeLeft(computeRemaining());

    const timer = setInterval(() => {
      if (isLocalGameOverRef.current) {
        clearInterval(timer);
        return;
      }
      const remaining = computeRemaining();
      setTimeLeft(remaining);
      if (remaining <= 0) {
        clearInterval(timer);
        setLocalGameOverRef.current();
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [gameState, endTimeEpochMs]);

  useEffect(() => {
    if (gameState !== 'PLAYING') return;

    // 게임 상태 초기화
    scoreRef.current = 0;
    setScore(0);
    setTimeLeft(GAME_DURATION);

    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const initialX = (canvas.width - INITIAL_BLOCK_WIDTH) / 2;
    stackRef.current = [{ x: initialX, width: INITIAL_BLOCK_WIDTH }];
    currentBlockRef.current = { x: initialX, width: INITIAL_BLOCK_WIDTH, direction: 1 };
    fallingPiecesRef.current = [];
    shakeRef.current = { intensity: 0, startTime: 0, duration: 0 };
    cameraYRef.current = canvas.height - 2 * BLOCK_HEIGHT;

    let rafId: number;

    const draw = (time: number) => {
      // 로컬 게임오버 시 루프 중단 (서버 DONE 대기)
      if (isLocalGameOverRef.current) return;

      const W = canvas.width;
      const H = canvas.height;

      ctx.clearRect(0, 0, W, H);

      // 배경 — 층수가 높아질수록 어두워짐
      const darken = Math.min(scoreRef.current / 40, 1);
      ctx.fillStyle = `rgb(${lerpInt(135, 15, darken)}, ${lerpInt(190, 15, darken)}, ${lerpInt(240, 60, darken)})`;
      ctx.fillRect(0, 0, W, H);

      // 화면 흔들림
      const shake = shakeRef.current;
      const shakeProgress =
        shake.duration > 0 ? Math.max(0, 1 - (time - shake.startTime) / shake.duration) : 0;
      const sx = (Math.random() * 2 - 1) * shake.intensity * shakeProgress;
      const sy = (Math.random() * 2 - 1) * shake.intensity * shakeProgress;

      ctx.save();
      ctx.translate(sx, sy);

      // 현재 블록 이동
      const stack = stackRef.current;
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

      // 동적 Y 좌표 (카메라 팔로우)
      const targetCameraY = Math.max(H / 2, H - (stack.length + 1) * BLOCK_HEIGHT);
      cameraYRef.current += (targetCameraY - cameraYRef.current) * 0.1;
      const movingBlockY = cameraYRef.current;

      // 쌓인 블록 드로우
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

      // 현재(움직이는) 블록 드로우
      ctx.shadowColor = 'rgba(0,0,0,0.4)';
      ctx.shadowBlur = 8;
      ctx.shadowOffsetY = 4;
      ctx.fillStyle = BLOCK_COLORS[stack.length % BLOCK_COLORS.length];
      ctx.fillRect(nx, movingBlockY, cur.width, BLOCK_HEIGHT - BLOCK_GAP);

      ctx.shadowColor = 'transparent';
      ctx.shadowBlur = 0;
      ctx.fillStyle = 'rgba(255,255,255,0.3)';
      ctx.fillRect(nx, movingBlockY, cur.width, 3);

      // 낙하 조각 드로우
      fallingPiecesRef.current = fallingPiecesRef.current
        .map((p) => ({
          ...p,
          y: p.y + p.vy,
          vy: p.vy + GRAVITY,
          opacity: Math.max(0, p.opacity - OPACITY_DECAY),
        }))
        .filter((p) => p.opacity > 0 && p.y < H + 100);

      fallingPiecesRef.current.forEach((p) => {
        ctx.globalAlpha = p.opacity;
        ctx.fillStyle = '#ef4444';
        ctx.fillRect(p.x, p.y, p.width, BLOCK_HEIGHT - BLOCK_GAP);
      });
      ctx.globalAlpha = 1;

      // 점수 텍스트
      ctx.fillStyle = 'rgba(255,255,255,0.9)';
      ctx.font = `bold 28px 'Pretendard Variable', Pretendard, sans-serif`;
      ctx.textAlign = 'center';
      ctx.fillText(`${scoreRef.current}층`, W / 2, 60);

      ctx.restore();

      rafId = requestAnimationFrame(draw);
    };

    rafId = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(rafId);
  }, [gameState, canvasRef]);

  return { score, timeLeft, handleTap };
};

// 외부에서 CANVAS_WIDTH를 참조할 수 있도록 재-export
export { CANVAS_WIDTH };
