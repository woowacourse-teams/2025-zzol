import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import GameIntroSlides from '../../components/GameIntroSlides/GameIntroSlides';

/**
 * 눈치게임 Ready 페이지 — 인트로 슬라이드 후 play 로 이동(BlockStacking 패턴).
 *
 * 주의: nunchi 는 PREPARE 상태가 없고 시작부터 PLAYING(currentNumber=1)이다(ADR 결정 8, N6).
 * 따라서 PLAYING 도달 시 play 로 이동한다.
 */
const NunchiGameReadyPage = () => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useIdentifier();
  const { gameState } = useNunchiGameContext();

  useEffect(() => {
    if (!joinCode) return;
    if (gameState === 'PLAYING' || gameState === 'COLLISION_COOLDOWN') {
      navigate(`/room/${joinCode}/NUNCHI_GAME/play`);
    }
  }, [gameState, joinCode, navigate]);

  return <GameIntroSlides gameType="NUNCHI_GAME" />;
};

export default NunchiGameReadyPage;
