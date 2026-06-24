import { useRef, useState, useCallback } from 'react';
import { RouletteSector, PlayerProbability } from '@/types/roulette';
import { convertProbabilitiesToAngles, interpolateAngles } from '../utils';

const ANIMATION_DURATION = 1600;

// Easing function - 빠른 시작, 부드러운 끝
const easeOutCubic = (t: number): number => {
  return 1 - Math.pow(1 - t, 3);
};

export const useRouletteTransition = (
  prev: PlayerProbability[] | null,
  current: PlayerProbability[] | null
) => {
  const [animatedSectors, setAnimatedSectors] = useState<RouletteSector[]>([]);
  const requestRef = useRef<number | null>(null);
  const startTimeRef = useRef<number | null>(null);

  const prevRef = useRef<PlayerProbability[]>([]);
  const currentRef = useRef<PlayerProbability[]>([]);
  prevRef.current = prev || [];
  currentRef.current = current || [];

  const startAnimationTransition = useCallback(() => {
    if (!prev || !current || prev.length === 0 || current.length === 0) {
      setAnimatedSectors([]);
      return;
    }

    const step = (timestamp: number) => {
      if (startTimeRef.current === null) startTimeRef.current = timestamp;

      const elapsed = timestamp - startTimeRef.current;
      const rawT = Math.min(elapsed / ANIMATION_DURATION, 1);
      const progress = easeOutCubic(rawT);

      const next = interpolateAngles({ from: prevRef.current, to: currentRef.current, progress });
      setAnimatedSectors(next);

      if (rawT < 1) {
        requestRef.current = requestAnimationFrame(step);
      } else {
        startTimeRef.current = null;
        if (requestRef.current) {
          cancelAnimationFrame(requestRef.current);
        }
      }
    };

    requestRef.current = requestAnimationFrame(step);
  }, [prev, current]);

  const stopAnimation = useCallback(() => {
    if (requestRef.current) {
      cancelAnimationFrame(requestRef.current);
      requestRef.current = null;
    }
    startTimeRef.current = null;
  }, []);

  const setToPrev = useCallback(() => {
    setAnimatedSectors(convertProbabilitiesToAngles(prev || []));
  }, [prev]);

  return {
    animatedSectors,
    startAnimation: startAnimationTransition,
    stopAnimation,
    setToPrev,
  };
};
