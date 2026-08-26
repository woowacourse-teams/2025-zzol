import ScreenReaderOnly from '@/components/@common/ScreenReaderOnly/ScreenReaderOnly';
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

const announcementOf = (isDead: boolean, followName: string, isFinished: boolean): string => {
  if (isFinished) return '게임이 종료되었습니다. 곧 결과로 이동합니다.';
  if (isDead) return `탈락했습니다. ${followName} 님을 관전 중입니다. 탭하면 대상을 전환합니다.`;
  return '';
};

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
  const isFinished = wormGameState === 'FINISH';
  const { isDead, followName, cycleFollow } = useWormSpectate(store, isPlaying);
  const { onPointerDown, onPointerMove } = useWormSteering({
    store,
    joinCode,
    playerName: myName,
    containerRef,
    // PREPARE 중에도 조준은 허용(스폰 직후 방향 유실 방지), 전송은 PLAYING·생존 중에만
    inputEnabled: !isDead && wormGameState !== 'DESCRIPTION',
    sendEnabled: isPlaying && !isDead,
  });

  useEffect(() => {
    store.setZoomOut(isFinished);
  }, [store, isFinished]);

  useEffect(() => {
    if (wormGameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [wormGameState, joinCode, miniGameType, navigate]);

  return (
    <S.Container
      ref={containerRef}
      role="application"
      aria-label="지렁이 게임 — 화면을 터치한 방향으로 조향합니다 (키보드 조작 미지원)"
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
    >
      <WormCanvas />
      {/* 상시 마운트 live region — 사망·종료를 스크린 리더에 전달 */}
      <ScreenReaderOnly aria-live="assertive">
        {announcementOf(isDead, followName, isFinished)}
      </ScreenReaderOnly>
      {wormGameState === 'PREPARE' && <PrepareOverlay />}
      {isPlaying && isDead && (
        <S.SpectateBar type="button" onClick={cycleFollow}>
          <span aria-hidden="true">👀 </span>
          {followName} 관전 중 · 탭해서 전환
        </S.SpectateBar>
      )}
      {isFinished && <S.FinishBadge role="status">게임 종료!</S.FinishBadge>}
    </S.Container>
  );
};

export default WormGamePlayPage;
