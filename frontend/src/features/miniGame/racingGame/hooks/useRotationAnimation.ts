import { usePrefersReducedMotion } from '@/hooks/usePrefersReducedMotion';
import { useEffect, useRef } from 'react';

const ROTATION_SPEED_MULTIPLIER = 22;
const SPEED_SMOOTHING_FACTOR = 0.08;
/** 이 아래로는 회전이 멈춰 보인다. 플레이어마다 루프가 하나씩 도니 스타일 쓰기를 건너뛴다. */
const MIN_RENDER_SPEED = 0.05;

type Props = {
  speed: number;
};

export const useRotationAnimation = ({ speed }: Props) => {
  const rotatingRef = useRef<HTMLDivElement>(null);
  const angleRef = useRef(0);
  const currentSpeedRef = useRef(0);
  const speedRef = useRef(speed);
  const prefersReducedMotion = usePrefersReducedMotion();

  useEffect(() => {
    speedRef.current = speed;
  }, [speed]);

  useEffect(() => {
    if (prefersReducedMotion) return;

    let frameId: number;
    let lastTime = performance.now();

    const update = (time: number) => {
      const delta = (time - lastTime) / 1000;
      lastTime = time;

      currentSpeedRef.current +=
        (speedRef.current - currentSpeedRef.current) * SPEED_SMOOTHING_FACTOR;

      if (currentSpeedRef.current > MIN_RENDER_SPEED) {
        angleRef.current += currentSpeedRef.current * delta * 10 * ROTATION_SPEED_MULTIPLIER;
        if (rotatingRef.current) {
          rotatingRef.current.style.transform = `rotate(${angleRef.current}deg)`;
        }
      }

      frameId = requestAnimationFrame(update);
    };

    frameId = requestAnimationFrame(update);
    return () => cancelAnimationFrame(frameId);
  }, [prefersReducedMotion]);

  return rotatingRef;
};
