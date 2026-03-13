import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useBlindTimerGame } from '@/contexts/BlindTimerGame/BlindTimerGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import GameIntroSlides from '../../components/GameIntroSlides/GameIntroSlides';

const BlindTimerGameReadyPage = () => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useIdentifier();
  const { gameState } = useBlindTimerGame();

  useEffect(() => {
    if (!joinCode) return;
    if (gameState === 'PREPARE') {
      navigate(`/room/${joinCode}/BLIND_TIMER/play`);
    }
  }, [gameState, joinCode, navigate]);

  return <GameIntroSlides gameType="BLIND_TIMER" />;
};

export default BlindTimerGameReadyPage;
