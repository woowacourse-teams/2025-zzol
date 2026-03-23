import { useMemo } from 'react';

const GRID_SIZE = 25;

const shuffleArray = (array: number[]): number[] => {
  const shuffled = [...array];
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  return shuffled;
};

export const useGridNumbers = () => {
  const numbers = useMemo(() => {
    const sequential = Array.from({ length: GRID_SIZE }, (_, i) => i + 1);
    return shuffleArray(sequential);
  }, []);

  return numbers;
};
