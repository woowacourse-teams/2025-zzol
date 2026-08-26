import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useWormGame } from '@/contexts/WormGame/WormGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import PrepareOverlay from '../../components/PrepareOverlay/PrepareOverlay';
import WormCanvas from '../components/WormCanvas/WormCanvas';
import { useWormSpectate } from '../hooks/useWormSpectate';
import { useWormSteering } from '../hooks/useWormSteering';
import * as S from './WormGamePlayPage.styled';

/**
 * 지렁이 게임 플레이. 시작은 로비의 공통 `/minigame/command` 가 이미 보냈으므로 여기서 보내지 않는다.
 * PREPARE 오버레이 → PLAYING(조향) → 사망 시 관전(탭으로 대상 순환) → FINISH 배지 → DONE 이면 결과로.
 */
const WormGamePlayPage = () => {
  const { joinCode, myName } = useIdentifier();
  const { miniGameType } = useParams();
  const navigate = useReplaceNavigate();
  const { wormGameState, store } = useWormGame();
  const containerRef = useRef<HTMLDivElement>(null);

  const isPlaying = wormGameState === 'PLAYING';
  const { isDead, followName, cycleFollow } = useWormSpectate(store, isPlaying);
  const { onPointerDown, onPointerMove } = useWormSteering({
    store,
    joinCode,
    playerName: myName,
    containerRef,
    enabled: isPlaying && !isDead,
  });

  useEffect(() => {
    if (wormGameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [wormGameState, joinCode, miniGameType, navigate]);

  return (
    <S.Container
      ref={containerRef}
      role="application"
      aria-label="지렁이 게임 — 화면을 터치한 방향으로 조향"
      onPointerDown={isDead ? undefined : onPointerDown}
      onPointerMove={isDead ? undefined : onPointerMove}
    >
      <WormCanvas />
      {wormGameState === 'PREPARE' && <PrepareOverlay />}
      {isPlaying && isDead && (
        <S.SpectateBar type="button" onClick={cycleFollow} aria-live="polite">
          👀 {followName} 관전 중 · 탭해서 전환
        </S.SpectateBar>
      )}
      {wormGameState === 'FINISH' && <S.FinishBadge role="status">게임 종료!</S.FinishBadge>}
    </S.Container>
  );
};

export default WormGamePlayPage;
