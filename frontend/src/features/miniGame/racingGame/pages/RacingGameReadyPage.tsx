import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useRacingGame } from '@/contexts/RacingGame/RacingGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import GameIntroSlides from '../../components/GameIntroSlides/GameIntroSlides';

const RacingGameReadyPage = () => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useIdentifier();
  const { racingGameState } = useRacingGame();

  useEffect(() => {
    if (!joinCode) return;
    if (racingGameState === 'PREPARE') {
      navigate(`/room/${joinCode}/RACING_GAME/play`);
    }
  }, [racingGameState, joinCode, navigate]);

  return <GameIntroSlides gameType="RACING_GAME" />;
};

export default RacingGameReadyPage;
