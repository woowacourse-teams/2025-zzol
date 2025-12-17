import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { RouletteWinnerResponse } from '@/types/roulette';
import { useEffect, useState } from 'react';

const useRoulettePlay = () => {
  const { joinCode, myName } = useIdentifier();
  const { send, stopSocket } = useWebSocket();
  const navigate = useReplaceNavigate();
  const [winner, setWinner] = useState<string | null>(null);
  const [randomAngle, setRandomAngle] = useState(0);
  const [isSpinStarted, setIsSpinStarted] = useState(false);

  const startSpinWithResult = (data: RouletteWinnerResponse) => {
    setWinner(data.playerName);
    setRandomAngle(data.randomAngle);
    setIsSpinStarted(true);
  };

  const handleSpinClick = () => {
    send(`/room/${joinCode}/spin-roulette`, { hostName: myName });
  };

  useEffect(() => {
    if (!isSpinStarted) return;

    if (!winner || !winner.trim()) {
      console.warn('당첨자가 추첨되지 않았습니다.');
    }

    const timer = setTimeout(() => {
      stopSocket();
      navigate(`/room/${joinCode}/roulette/result`, { state: { winner } });
    }, 5000);

    return () => clearTimeout(timer);
  }, [isSpinStarted, winner, navigate, joinCode, stopSocket]);

  return {
    winner,
    randomAngle,
    isSpinStarted,
    handleSpinClick,
    startSpinWithResult,
  };
};

export default useRoulettePlay;
