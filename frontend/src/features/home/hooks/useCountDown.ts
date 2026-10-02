import { useEffect, useState } from 'react';

const DURATION_MS = 1100;
const easeOutCubic = (t: number) => 1 - (1 - t) ** 3;
const prefersReducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/**
 * 마운트 시 100 에서 target 까지 정수를 내려 세는 숫자를 돌려준다.
 * 모션 줄이기 설정이면 처음부터 target 이다. target 이 바뀌면 다시 센다.
 */
export const useCountDown = (target: number) => {
  const [value, setValue] = useState(() => (prefersReducedMotion() ? target : 100));

  useEffect(() => {
    if (prefersReducedMotion()) return;
    const start = performance.now();
    let frame = 0;
    const tick = (now: number) => {
      const progress = Math.min(1, (now - start) / DURATION_MS);
      setValue(Math.round(100 - (100 - target) * easeOutCubic(progress)));
      if (progress < 1) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [target]);

  return value;
};
