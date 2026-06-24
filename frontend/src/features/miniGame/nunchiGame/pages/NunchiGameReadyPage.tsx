import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect } from 'react';
import GameIntroSlides from '../../components/GameIntroSlides/GameIntroSlides';

/**
 * 눈치게임 Ready 페이지 — 인트로 슬라이드 후 play 로 이동(BlockStacking 패턴).
 *
 * 전환 규칙(ADR 결정 8): 서버는 진입 시 DESCRIPTION(playStartEpochMs 포함)을 보내고,
 * playStartEpochMs 에 PLAYING 으로 전이한다. FE 는 PLAYING/COLLISION 도착 시 play 로 가되,
 * PLAYING 메시지를 놓쳐도(구독 레이스) playStartEpochMs 타임아웃으로 결정적으로 넘어간다.
 * 종료 후 늦은 진입(DONE 스냅샷)은 result 로 보낸다.
 */
const NunchiGameReadyPage = () => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useIdentifier();
  const { gameState, playStartEpochMs, serverOffsetMs } = useNunchiGameContext();

  useEffect(() => {
    if (!joinCode) return;
    if (gameState === 'PLAYING' || gameState === 'COLLISION_COOLDOWN') {
      navigate(`/room/${joinCode}/NUNCHI_GAME/play`);
      return;
    }
    if (gameState === 'DONE') {
      navigate(`/room/${joinCode}/NUNCHI_GAME/result`);
    }
  }, [gameState, joinCode, navigate]);

  // DESCRIPTION 동안 playStartEpochMs 까지 인트로를 보여주고 그 시각에 play 로 전환(PLAYING 놓침 안전장치).
  // PLAYING 이 먼저 도착하면 위 effect 가 navigate → 언마운트되며 이 타이머는 cleanup 으로 해제된다.
  useEffect(() => {
    if (!joinCode) return;
    if (gameState !== 'DESCRIPTION' || playStartEpochMs == null) return;

    const delay = Math.max(0, playStartEpochMs - (Date.now() + serverOffsetMs));
    const timerId = setTimeout(() => {
      navigate(`/room/${joinCode}/NUNCHI_GAME/play`);
    }, delay);

    return () => clearTimeout(timerId);
  }, [gameState, playStartEpochMs, serverOffsetMs, joinCode, navigate]);

  return <GameIntroSlides gameType="NUNCHI_GAME" />;
};

export default NunchiGameReadyPage;
