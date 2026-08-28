import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useWormGame } from '@/contexts/WormGame/WormGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import GameIntroSlides from '../../components/GameIntroSlides/GameIntroSlides';

/** 서버 state 만 보고 전이한다. 늦게 진입해도 PLAYING/FINISH 는 play, DONE 은 result 로 복구 */
const WormGameReadyPage = () => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useIdentifier();
  const { wormGameState } = useWormGame();

  useEffect(() => {
    if (!joinCode) return;
    if (wormGameState === 'DONE') {
      navigate(`/room/${joinCode}/WORM_GAME/result`);
    } else if (wormGameState !== 'DESCRIPTION') {
      navigate(`/room/${joinCode}/WORM_GAME/play`);
    }
  }, [wormGameState, joinCode, navigate]);

  return <GameIntroSlides gameType="WORM_GAME" />;
};

export default WormGameReadyPage;
