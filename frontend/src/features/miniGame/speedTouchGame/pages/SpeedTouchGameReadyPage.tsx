import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useSpeedTouchGame } from '@/contexts/SpeedTouchGame/SpeedTouchGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import GameIntroSlides from '../../components/GameIntroSlides/GameIntroSlides';

const SpeedTouchGameReadyPage = () => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useIdentifier();
  const { gameState } = useSpeedTouchGame();

  useEffect(() => {
    if (!joinCode) return;
    if (gameState === 'PREPARE') {
      navigate(`/room/${joinCode}/SPEED_TOUCH/play`);
    }
  }, [gameState, joinCode, navigate]);

  return <GameIntroSlides gameType="SPEED_TOUCH" />;
};

export default SpeedTouchGameReadyPage;
