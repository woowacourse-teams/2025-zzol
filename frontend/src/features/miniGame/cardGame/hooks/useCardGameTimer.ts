import { useCardGame } from '@/contexts/CardGame/CardGameContext';
import { useEffect, useState } from 'react';

const ROUND_TOTAL_TIME = 10;

export const useCardGameTimer = () => {
  const { currentCardGameState, currentRound } = useCardGame();
  const [currentTime, setCurrentTime] = useState(ROUND_TOTAL_TIME);
  const [isTimerActive, setIsTimerActive] = useState(false);

  useEffect(() => {
    if (!isTimerActive || currentTime <= 0) return;

    const timer = setTimeout(() => {
      setCurrentTime((prev) => prev - 1);
    }, 1000);

    return () => clearTimeout(timer);
  }, [currentTime, isTimerActive]);

  useEffect(() => {
    const isPrepareState = currentCardGameState === 'PREPARE';
    const isPlayingState = currentCardGameState === 'PLAYING';

    if (isPrepareState) {
      setCurrentTime(ROUND_TOTAL_TIME);
      setIsTimerActive(false);
      return;
    }

    if (isPlayingState) {
      setCurrentTime(ROUND_TOTAL_TIME);
      setIsTimerActive(true);
    }
  }, [currentCardGameState, currentRound]);

  return {
    currentTime,
    isTimerActive,
    roundTotalTime: ROUND_TOTAL_TIME,
  };
};
