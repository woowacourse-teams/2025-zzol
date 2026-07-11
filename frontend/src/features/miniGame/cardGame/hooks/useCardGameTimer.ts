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

  // 게임 상태·라운드 전환 시 타이머를 리셋한다(렌더 중 state 조정 — 가드로 전환 시 1회만).
  // sentinel('')로 초기화해 마운트 시(예: PLAYING 상태로 재진입)에도 1회 실행되도록 한다.
  const stateKey = `${currentCardGameState}:${currentRound}`;
  const [prevStateKey, setPrevStateKey] = useState('');
  if (stateKey !== prevStateKey) {
    setPrevStateKey(stateKey);
    if (currentCardGameState === 'PREPARE') {
      setCurrentTime(ROUND_TOTAL_TIME);
      setIsTimerActive(false);
    } else if (currentCardGameState === 'PLAYING') {
      setCurrentTime(ROUND_TOTAL_TIME);
      setIsTimerActive(true);
    }
  }

  return {
    currentTime,
    isTimerActive,
    roundTotalTime: ROUND_TOTAL_TIME,
  };
};
