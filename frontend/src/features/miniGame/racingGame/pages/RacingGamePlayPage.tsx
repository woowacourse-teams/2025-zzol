import ScreenReaderOnly from '@/components/@common/ScreenReaderOnly/ScreenReaderOnly';
import { colorList } from '@/constants/color';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { useRacingGame } from '@/contexts/RacingGame/RacingGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useCallback, useEffect, useMemo, useRef } from 'react';
import { useParams } from 'react-router-dom';
import PrepareOverlay from '../../components/PrepareOverlay/PrepareOverlay';
import Finish from '../components/Finish/Finish';
import Goal from '../components/Goal/Goal';
import HiddenPlayersBadge, {
  HiddenPlayersSummary,
} from '../components/HiddenPlayersBadge/HiddenPlayersBadge';
import RacingGameOverlay from '../components/RacingGameOverlay/RacingGameOverlay';
import RacingLine from '../components/RacingLine/RacingLine';
import RacingPlayer from '../components/RacingPlayer/RacingPlayer';
import RacingProgressBar from '../components/RacingProgressBar/RacingProgressBar';
import RacingRanks from '../components/RacingRanks/RacingRanks';
import TrackNotice from '../components/TrackNotice/TrackNotice';
import { useBackgroundAnimation } from '../hooks/useBackgroundAnimation';
import { useRaceAnnouncement } from '../hooks/useRaceAnnouncement';
import { useRaceRanking } from '../hooks/useRaceRanking';
import { useGoalDisplay } from '../hooks/useGoalDisplay';
import { usePlayerData } from '../hooks/usePlayerData';
import { getVisiblePlayers } from '../utils/getVisiblePlayers';
import * as S from './RacingGamePlayPage.styled';

const FINISH_LINE_VISUAL_OFFSET = 30;
const STUCK_TIMEOUT_MS = 8000;
/** DONE 을 받고 결과 화면으로 넘어가기까지 완주 연출을 보여 주는 시간. */
const FINISH_DISPLAY_MS = 2000;
const TAP_POP_SCALE = 1.06;
const TAP_POP_DURATION_MS = 160;

const RacingGamePage = () => {
  const { joinCode, myName } = useIdentifier();
  const navigate = useReplaceNavigate();
  const { miniGameType } = useParams();
  const { racingGameState, racingGameData } = useRacingGame();
  const { getParticipantColorIndex } = useParticipants();

  const containerRef = useRef<HTMLDivElement | null>(null);
  const myPlayerRef = useRef<HTMLDivElement | null>(null);
  const tapPopRef = useRef<Animation | null>(null);

  // 탭한 순간 내 캐릭터를 한 번 튕긴다. 리렌더 없이 그리려고 Web Animations API 로 직접 건다.
  const handleTap = useCallback(() => {
    tapPopRef.current?.cancel();
    tapPopRef.current =
      myPlayerRef.current?.animate?.(
        [
          { transform: 'scale(1)' },
          { transform: `scale(${TAP_POP_SCALE})` },
          { transform: 'scale(1)' },
        ],
        { duration: TAP_POP_DURATION_MS, easing: 'ease-out' }
      ) ?? null;
  }, []);

  const { rows, hiddenAhead, hiddenBehind, isSpectating } = useMemo(
    () => getVisiblePlayers(racingGameData.players, myName),
    [racingGameData.players, myName]
  );

  // 잘라낸 목록이 아니라 원본에서 내 위치를 찾는다. 자르는 기준이 바뀌어도 내 좌표는 흔들리지 않는다.
  const { myPosition, mySpeed } = usePlayerData({
    players: racingGameData.players,
    myName,
  });

  const rankedPlayers = useRaceRanking({
    players: racingGameData.players,
    endDistance: racingGameData.distance.end,
  });

  const isGoal = useGoalDisplay({
    myPosition,
    endDistance: racingGameData.distance.end,
  });

  useBackgroundAnimation({
    containerRef,
    mySpeed,
  });

  // 낭독도 화면 순위표와 같은 순서를 쓴다. position 으로 따로 세면 완주자가 감속하는 동안 등수가 갈린다.
  const myRank = rankedPlayers.findIndex((player) => player.playerName === myName) + 1;

  const announcement = useRaceAnnouncement({
    rank: myRank,
    totalPlayers: racingGameData.players.length,
    remainingDistance: Math.max(0, Math.round(racingGameData.distance.end - myPosition)),
    enabled: racingGameState === 'PLAYING' && myRank > 0,
  });

  // DONE 을 받은 프레임에 바로 navigate 하면 완주 연출이 한 프레임 스치고 사라진다.
  useEffect(() => {
    if (racingGameState !== 'DONE') return;

    const timer = window.setTimeout(() => {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }, FINISH_DISPLAY_MS);
    return () => window.clearTimeout(timer);
  }, [racingGameState, joinCode, navigate, miniGameType]);

  // stuck 폴백: 시작을 알리는 상태가 안 와 화면이 멈춰 있으면 로비로 보낸다. 눈치게임과 같은 기준이다.
  // 진행 중(PLAYING)과 종료(DONE)는 화면이 정상이므로 감시하지 않는다.
  // 상태는 이벤트로만 오고 스냅샷이 없다. 경기 도중 새로고침하면 기본값 DESCRIPTION 에 머무는데,
  // 위치는 100ms 마다 계속 오므로 트랙은 정상이다. 위치까지 안 올 때만 멈춘 것으로 본다.
  const hasTrackData = racingGameData.players.length > 0;

  useEffect(() => {
    if (racingGameState !== 'DESCRIPTION' && racingGameState !== 'PREPARE') return;
    if (hasTrackData) return;

    const timer = window.setTimeout(() => {
      navigate(`/room/${joinCode}/lobby`);
    }, STUCK_TIMEOUT_MS);
    return () => window.clearTimeout(timer);
  }, [racingGameState, hasTrackData, joinCode, navigate]);

  return (
    <>
      {racingGameState === 'PREPARE' && <PrepareOverlay />}
      {racingGameState === 'DONE' && <Finish />}
      {isGoal && racingGameState === 'PLAYING' && <Goal />}
      <ScreenReaderOnly aria-live="polite">{announcement}</ScreenReaderOnly>
      <RacingGameOverlay isGoal={isGoal} onTap={handleTap}>
        <S.Container ref={containerRef}>
          <RacingProgressBar
            myName={myName}
            endDistance={racingGameData.distance.end}
            players={racingGameData.players}
          />
          <RacingRanks
            rankedPlayers={rankedPlayers}
            myName={myName}
            endDistance={racingGameData.distance.end}
          />
          <S.ContentWrapper>
            <S.PlayersWrapper>
              {/* 출발선 */}
              <RacingLine position={racingGameData.distance.start} myPosition={myPosition} />
              {/* 도착선 — 결승선 거리를 받기 전에는 그리지 않는다 */}
              {racingGameData.distance.end > 0 && (
                <RacingLine
                  position={racingGameData.distance.end - FINISH_LINE_VISUAL_OFFSET}
                  myPosition={myPosition}
                />
              )}
              {rows.map(({ player, slot, isVisible }) => (
                <RacingPlayer
                  key={player.playerName}
                  ref={player.playerName === myName ? myPlayerRef : undefined}
                  player={player}
                  isMe={player.playerName === myName}
                  myPosition={myPosition}
                  color={colorList[getParticipantColorIndex(player.playerName)]}
                  slot={slot}
                  isVisible={isVisible}
                />
              ))}
              <HiddenPlayersBadge direction="ahead" count={hiddenAhead} />
              <HiddenPlayersBadge direction="behind" count={hiddenBehind} />
              <HiddenPlayersSummary
                totalCount={racingGameData.players.length}
                visibleCount={rows.filter(({ isVisible }) => isVisible).length}
              />
              {(isSpectating || racingGameData.players.length === 0) && <TrackNotice />}
            </S.PlayersWrapper>
          </S.ContentWrapper>
        </S.Container>
      </RacingGameOverlay>
    </>
  );
};

export default RacingGamePage;
