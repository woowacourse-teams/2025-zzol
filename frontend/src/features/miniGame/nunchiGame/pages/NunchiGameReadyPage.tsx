import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import GameIntroSlides from '../../components/GameIntroSlides/GameIntroSlides';

/**
 * 눈치게임 Ready 페이지 — 인트로 슬라이드(DESCRIPTION 단계) 표시.
 *
 * 전환 규칙(ADR 결정 9): 서버는 DESCRIPTION(규칙 설명) → READY(곧 시작 카운트다운, playStartEpochMs
 * 포함) → PLAYING 순으로 보낸다. FE 는 READY 진입 즉시 play 로 이동하고, "곧 시작 카운트다운(READY→
 * START!)"은 게임 화면 위 PrepareOverlay 로 보여준다(카드게임 패턴 — 설명 슬라이드 위가 아님).
 * COLLISION 도착도 play 로, 종료 후 늦은 진입(DONE)은 result 로 보낸다.
 */
const NunchiGameReadyPage = () => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useIdentifier();
  const { gameState } = useNunchiGameContext();

  useEffect(() => {
    if (!joinCode) return;
    if (gameState === 'READY' || gameState === 'PLAYING' || gameState === 'COLLISION_COOLDOWN') {
      navigate(`/room/${joinCode}/NUNCHI_GAME/play`);
      return;
    }
    if (gameState === 'DONE') {
      navigate(`/room/${joinCode}/NUNCHI_GAME/result`);
    }
  }, [gameState, joinCode, navigate]);

  return <GameIntroSlides gameType="NUNCHI_GAME" />;
};

export default NunchiGameReadyPage;
