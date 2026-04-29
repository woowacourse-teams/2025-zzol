import { useLadderGameContext } from '@/contexts/LadderGame/LadderGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import GameIntroSlides from '../../components/GameIntroSlides/GameIntroSlides';

const LadderGameReadyPage = () => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useIdentifier();
  const { gameState } = useLadderGameContext();

  useEffect(() => {
    if (!joinCode) return;
    // DESCRIPTION 동안은 슬라이드 유지, PREPARE부터 PlayPage로 이동
    // PREPARE를 놓쳤을 경우 DRAWING, RESULT로 폴백
    if (['PREPARE', 'DRAWING', 'RESULT'].includes(gameState)) {
      navigate(`/room/${joinCode}/LADDER_GAME/play`);
    }
  }, [gameState, joinCode, navigate]);

  return <GameIntroSlides gameType="LADDER_GAME" />;
};

export default LadderGameReadyPage;
