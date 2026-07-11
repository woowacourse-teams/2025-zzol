import { PlayerProbability } from '@/types/roulette';
import { useEffect, useState } from 'react';

export const useRouletteScreenReader = (playerProbabilities: PlayerProbability[]) => {
  const [message, setMessage] = useState('');

  useEffect(() => {
    const initialMessage = `룰렛 화면입니다. 미니게임을 통해 당첨 확률이 조정됩니다. ${formatProbabilitiesForScreenReader(
      playerProbabilities
    )}`;
    // aria-live 영역: 확률 변경 시 낭독 트리거를 위해 effect 에서 메시지를 갱신한다.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMessage(initialMessage);
  }, [playerProbabilities]);

  const updateViewMessage = () => {
    setMessage(formatProbabilitiesForScreenReader(playerProbabilities));
  };

  return {
    message,
    updateViewMessage,
  };
};

const formatProbabilitiesForScreenReader = (playerProbabilities: PlayerProbability[]): string => {
  if (playerProbabilities.length === 0) {
    return '현재 참여한 인원이 없습니다.';
  }

  return playerProbabilities
    .map(({ playerName, probability }) => `${playerName}님의 확률 ${probability}%`)
    .join(', ');
};
