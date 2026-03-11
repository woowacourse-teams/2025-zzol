import { useEffect, useRef, useState } from 'react';

const TOTAL_SECONDS = 30;
const INTERVAL_MS = 100;

export const useCountdown = (isRunning: boolean) => {
  const [remainingMs, setRemainingMs] = useState(TOTAL_SECONDS * 1000);
  const startTimeRef = useRef<number | null>(null);

  useEffect(() => {
    if (!isRunning) return;

    startTimeRef.current = Date.now();

    const timer = setInterval(() => {
      const elapsed = Date.now() - (startTimeRef.current ?? Date.now());
      const remaining = Math.max(TOTAL_SECONDS * 1000 - elapsed, 0);
      setRemainingMs(remaining);

      if (remaining <= 0) {
        clearInterval(timer);
      }
    }, INTERVAL_MS);

    return () => clearInterval(timer);
  }, [isRunning]);

  const seconds = Math.ceil(remainingMs / 1000);
  const progress = remainingMs / (TOTAL_SECONDS * 1000);

  return { seconds, progress, totalSeconds: TOTAL_SECONDS };
};
