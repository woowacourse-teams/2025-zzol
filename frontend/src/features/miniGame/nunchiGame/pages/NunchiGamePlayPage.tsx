import Headline4 from '@/components/@common/Headline4/Headline4';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import { NunchiGameState } from '@/types/miniGame/nunchiGame';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import PrepareOverlay from '@/features/miniGame/components/PrepareOverlay/PrepareOverlay';
import Layout from '@/layouts/Layout';
import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useNunchiCountdown } from '../hooks/useNunchiCountdown';
import NunchiStage from '../components/NunchiStage/NunchiStage';
import NunchiKeypad from '../components/NunchiKeypad/NunchiKeypad';
import NunchiEliminatedBanner from '../components/NunchiEliminatedBanner/NunchiEliminatedBanner';
import NunchiEliminatedOverlay from '../components/NunchiEliminatedOverlay/NunchiEliminatedOverlay';
import * as S from './NunchiGamePlayPage.styled';

/** playStart 이 시각만큼 남았을 때 START! 로 전환한다(요청: 0.5초 전). 그 전 READY 구간은 'READY'. */
const START_LEAD_MS = 500;

/** 메시지 미수신(이미 종료 후 직접 진입 등)으로 진행 상태가 안 와 stuck 으로 간주하는 한계(ms). */
const HOLDING_TIMEOUT_MS = 8000;
/** READY 에서 playStart 가 지난 뒤 PLAYING 을 기다리는 유예(ms). 이후에도 안 오면 stuck. */
const PLAYING_GRACE_MS = 3000;

/**
 * stuck 감지 타임아웃(ms) 또는 감시 안 함(null).
 *
 * 정상적으로 화면이 유지되는 진행 상태(PLAYING·COLLISION_COOLDOWN·DONE)는 감시하지 않는다.
 *  - DESCRIPTION: play 라우트엔 직접 진입(새로고침)뿐이고 곧 스냅샷/복구로 진행돼야 한다. 안 오면 stuck.
 *  - READY: playStart(스큐 보정) 이후 유예까지 PLAYING 이 안 오면(메시지 유실) stuck.
 * 재연결 복구(WebSocketProvider)가 먼저 진행시키면 상태가 바뀌어 타이머가 해제된다.
 */
const holdingTimeoutMs = (
  gameState: NunchiGameState,
  playStartEpochMs: number | null,
  serverOffsetMs: number
): number | null => {
  if (gameState === 'DESCRIPTION') return HOLDING_TIMEOUT_MS;
  if (gameState === 'READY' && playStartEpochMs != null) {
    const untilStartMs = Math.max(0, playStartEpochMs - (Date.now() + serverOffsetMs));
    return untilStartMs + PLAYING_GRACE_MS;
  }
  return null;
};

/**
 * 눈치게임 Play 페이지.
 *
 * 화면 구성(ADR 컨텍스트): 상단 = 현재 숫자/일어선 사람/일어서기·충돌 애니메이션,
 * 하단 = 입력 키패드.
 *
 * TODO(구현 — 컴포넌트 조립):
 *  - <NunchiStage/>: 현재 숫자(currentNumber) + 일어선 사람(stood, "일어섰다"만 — H)
 *    + 충돌 그룹(collided) 흔들림/빨강 애니메이션 + 쿨다운 재개 카운트다운(resumeAtEpochMs).
 *  - <NunchiKeypad/>: 큰 타겟 버튼, press() 호출, canPress 로 비활성, 끊김 경고(J).
 *  - useNunchiCountdown 으로 idle/hardCap·resumeAt 카운트다운 표시(G).
 *  - DONE 시 결과 페이지로 navigate(BlockStacking 패턴, 아래 useEffect).
 */
const NunchiGamePlayPage = () => {
  const { joinCode } = useIdentifier();
  const { gameState, playStartEpochMs, serverOffsetMs } = useNunchiGameContext();
  const navigate = useReplaceNavigate();
  const { miniGameType } = useParams();

  useEffect(() => {
    if (gameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [gameState, joinCode, navigate, miniGameType]);

  // stuck 폴백: 진행 상태가 안 와 멈춰 있으면(8·9번 — READY 후 PLAYING 유실 / 종료 후 직접 진입)
  // 재연결 복구로도 풀리지 않은 것이므로 로비로 이탈시킨다. 정상 진행 상태에선 감시하지 않는다.
  useEffect(() => {
    const delayMs = holdingTimeoutMs(gameState, playStartEpochMs, serverOffsetMs);
    if (delayMs == null) return;
    const timer = window.setTimeout(() => {
      navigate(`/room/${joinCode}/lobby`);
    }, delayMs);
    return () => clearTimeout(timer);
  }, [gameState, playStartEpochMs, serverOffsetMs, joinCode, navigate]);

  // playStart 까지 남은 시간(스큐 보정). READY 동안만 카운트한다.
  const startRemainingMs = useNunchiCountdown(
    playStartEpochMs,
    serverOffsetMs,
    gameState === 'READY'
  );

  // READY(곧 시작 카운트다운) 동안 카드게임 PrepareOverlay 를 게임 화면 위에 띄운다.
  const showPrepareOverlay = gameState === 'READY' && playStartEpochMs != null;
  // playStart 0.5초 전부터 START!. remaining===0(첫 프레임 전/만료)일 땐 READY 로 둬 START 깜빡임 방지.
  const preparePhase =
    startRemainingMs > 0 && startRemainingMs <= START_LEAD_MS ? 'START' : 'READY';

  return (
    <Layout>
      <Layout.TopBar center={<Headline4>눈치게임</Headline4>} />
      <Layout.Content>
        <S.Container>
          {/* 쿨다운 중엔 무대 "충돌!" 연출이 이번 그룹을 크게 보여주므로 배너는 숨긴다(이중표시 방지). */}
          {gameState !== 'COLLISION_COOLDOWN' && <NunchiEliminatedBanner />}
          <NunchiStage />
          <NunchiKeypad />
          <NunchiEliminatedOverlay />
        </S.Container>
      </Layout.Content>
      {showPrepareOverlay && <PrepareOverlay phase={preparePhase} />}
    </Layout>
  );
};

export default NunchiGamePlayPage;
