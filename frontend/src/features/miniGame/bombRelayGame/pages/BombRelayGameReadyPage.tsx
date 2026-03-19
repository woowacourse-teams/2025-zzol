import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useBombRelayGame } from '@/contexts/BombRelayGame/BombRelayGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import GameIntroSlides from '../../components/GameIntroSlides/GameIntroSlides';

const BombRelayGameReadyPage = () => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useIdentifier();
  const { gameState } = useBombRelayGame();

  useEffect(() => {
    if (!joinCode) return;
    if (gameState === 'PREPARE') {
      navigate(`/room/${joinCode}/BOMB_RELAY/play`);
    }
  }, [gameState, joinCode, navigate]);

  return <GameIntroSlides gameType="BOMB_RELAY" />;
};

export default BombRelayGameReadyPage;
