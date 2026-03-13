import { useEffect, useRef, useState } from 'react';

const INTERVAL_MS = 10;

export const useBlindTimer = (isRunning: boolean, blindDelayMillis: number) => {
  const [elapsedMs, setElapsedMs] = useState(0);
  const startTimeRef = useRef<number | null>(null);

  useEffect(() => {
    if (!isRunning) return;

    startTimeRef.current = Date.now();

    const timer = setInterval(() => {
      const elapsed = Date.now() - (startTimeRef.current ?? Date.now());
      setElapsedMs(elapsed);
    }, INTERVAL_MS);

    return () => clearInterval(timer);
  }, [isRunning]);

  const isBlind = elapsedMs >= blindDelayMillis;

  const formatTime = (ms: number): string => {
    const seconds = Math.floor(ms / 1000);
    const centiseconds = Math.floor((ms % 1000) / 10);
    return `${seconds}.${centiseconds.toString().padStart(2, '0')}`;
  };

  const displayTime = isBlind ? '??.??' : formatTime(elapsedMs);

  return { elapsedMs, isBlind, displayTime, formatTime };
};
