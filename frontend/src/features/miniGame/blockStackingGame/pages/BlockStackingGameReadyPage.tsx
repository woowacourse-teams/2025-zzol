import { useBlockStackingGameContext } from '@/contexts/BlockStackingGame/BlockStackingGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import GameIntroSlides from '../../components/GameIntroSlides/GameIntroSlides';

const BlockStackingGameReadyPage = () => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useIdentifier();
  const { gameState } = useBlockStackingGameContext();

  useEffect(() => {
    if (!joinCode) return;
    // PREPARE가 구독 세팅 전에 도달해 놓쳐도, PLAYING(3초 후)으로 폴백 이동
    if (gameState === 'PREPARE' || gameState === 'PLAYING') {
      navigate(`/room/${joinCode}/BLOCK_STACKING/play`);
    }
  }, [gameState, joinCode, navigate]);

  return <GameIntroSlides gameType="BLOCK_STACKING" />;
};

export default BlockStackingGameReadyPage;
