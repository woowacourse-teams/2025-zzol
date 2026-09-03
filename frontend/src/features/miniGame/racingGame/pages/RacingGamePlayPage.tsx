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
import HiddenPlayersBadge from '../components/HiddenPlayersBadge/HiddenPlayersBadge';
import RacingGameOverlay from '../components/RacingGameOverlay/RacingGameOverlay';
import RacingLine from '../components/RacingLine/RacingLine';
import RacingPlayer from '../components/RacingPlayer/RacingPlayer';
import RacingProgressBar from '../components/RacingProgressBar/RacingProgressBar';
import RacingRanks from '../components/RacingRanks/RacingRanks';
import TrackNotice from '../components/TrackNotice/TrackNotice';
import { useBackgroundAnimation } from '../hooks/useBackgroundAnimation';
import { useRaceAnnouncement } from '../hooks/useRaceAnnouncement';
import { useGoalDisplay } from '../hooks/useGoalDisplay';
import { usePlayerData } from '../hooks/usePlayerData';
import { getVisiblePlayers } from '../utils/getVisiblePlayers';
import * as S from './RacingGamePlayPage.styled';

const FINISH_LINE_VISUAL_OFFSET = 30;
const STUCK_TIMEOUT_MS = 8000;
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

  const {
    players: visiblePlayers,
    hiddenAhead,
    hiddenBehind,
    isSpectating,
  } = useMemo(
    () => getVisiblePlayers(racingGameData.players, myName),
    [racingGameData.players, myName]
  );

  // 잘라낸 목록이 아니라 원본에서 내 위치를 찾는다. 자르는 기준이 바뀌어도 내 좌표는 흔들리지 않는다.
  const { myPosition, mySpeed } = usePlayerData({
    players: racingGameData.players,
    myName,
  });

  const isGoal = useGoalDisplay({
    myPosition,
    endDistance: racingGameData.distance.end,
  });

  useBackgroundAnimation({
    containerRef,
    mySpeed,
  });

  const announcement = useRaceAnnouncement({
    rank: racingGameData.players.filter((player) => player.position > myPosition).length + 1,
    totalPlayers: racingGameData.players.length,
    remainingDistance: Math.max(0, Math.round(racingGameData.distance.end - myPosition)),
    enabled: racingGameState === 'PLAYING',
  });

  useEffect(() => {
    if (racingGameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [racingGameState, joinCode, navigate, miniGameType]);

  // stuck 폴백: 시작을 알리는 상태가 안 와 화면이 멈춰 있으면 로비로 보낸다. 눈치게임과 같은 기준이다.
  // 진행 중(PLAYING)과 종료(DONE)는 화면이 정상이므로 감시하지 않는다.
  useEffect(() => {
    if (racingGameState !== 'DESCRIPTION' && racingGameState !== 'PREPARE') return;

    const timer = window.setTimeout(() => {
      navigate(`/room/${joinCode}/lobby`);
    }, STUCK_TIMEOUT_MS);
    return () => window.clearTimeout(timer);
  }, [racingGameState, joinCode, navigate]);

  return (
    <>
      {racingGameState === 'PREPARE' && <PrepareOverlay />}
      {racingGameState === 'DONE' && <Finish />}
      {isGoal && racingGameState === 'PLAYING' && <Goal />}
      <ScreenReaderOnly aria-live="polite">{announcement}</ScreenReaderOnly>
      <RacingGameOverlay isGoal={isGoal} onTap={handleTap}>
        <S.Container ref={containerRef}>
          <RacingRanks
            players={racingGameData.players}
            myName={myName}
            endDistance={racingGameData.distance.end}
          />
          <RacingProgressBar
            myName={myName}
            endDistance={racingGameData.distance.end}
            players={racingGameData.players}
          />
          <S.ContentWrapper>
            <S.PlayersWrapper>
              {/* 출발선 */}
              <RacingLine position={racingGameData.distance.start} myPosition={myPosition} />
              {/* 도착선 */}
              <RacingLine
                position={racingGameData.distance.end - FINISH_LINE_VISUAL_OFFSET}
                myPosition={myPosition}
              />
              {visiblePlayers.map((player) => (
                <RacingPlayer
                  key={player.playerName}
                  ref={player.playerName === myName ? myPlayerRef : undefined}
                  player={player}
                  isMe={player.playerName === myName}
                  myPosition={myPosition}
                  color={colorList[getParticipantColorIndex(player.playerName)]}
                />
              ))}
              <HiddenPlayersBadge direction="ahead" count={hiddenAhead} />
              <HiddenPlayersBadge direction="behind" count={hiddenBehind} />
              {isSpectating && <TrackNotice />}
            </S.PlayersWrapper>
          </S.ContentWrapper>
        </S.Container>
      </RacingGameOverlay>
    </>
  );
};

export default RacingGamePage;
