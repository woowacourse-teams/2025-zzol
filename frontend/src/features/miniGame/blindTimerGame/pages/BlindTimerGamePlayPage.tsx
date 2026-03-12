import Headline4 from '@/components/@common/Headline4/Headline4';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useBlindTimerGame } from '@/contexts/BlindTimerGame/BlindTimerGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import PrepareOverlay from '../../components/PrepareOverlay/PrepareOverlay';
import TargetTime from '../components/TargetTime/TargetTime';
import TimerDisplay from '../components/TimerDisplay/TimerDisplay';
import PlayerStatusBoard from '../components/PlayerStatusBoard/PlayerStatusBoard';
import { useBlindTimer } from '../hooks/useBlindTimer';
import { useBlindTimerActions } from '../hooks/useBlindTimerActions';
import * as S from './BlindTimerGamePlayPage.styled';

const BlindTimerGamePlayPage = () => {
  const { joinCode, myName } = useIdentifier();
  const { gameState, targetTimeMillis, blindDelayMillis, progressData } = useBlindTimerGame();
  const { sendStop } = useBlindTimerActions();
  const navigate = useReplaceNavigate();
  const { miniGameType } = useParams();

  const [isStopped, setIsStopped] = useState(false);
  const stoppedTimeRef = useRef<number | null>(null);

  const isPlaying = gameState === 'PLAYING';
  const { elapsedMs, isBlind, displayTime, formatTime } = useBlindTimer(
    isPlaying,
    blindDelayMillis
  );

  const handleStop = useCallback(() => {
    if (isStopped || !isPlaying) return;
    stoppedTimeRef.current = elapsedMs;
    setIsStopped(true);
    sendStop();
  }, [isStopped, isPlaying, elapsedMs, sendStop]);

  useEffect(() => {
    if (gameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [gameState, joinCode, navigate, miniGameType]);

  const stoppedTimeDisplay =
    stoppedTimeRef.current !== null ? formatTime(stoppedTimeRef.current) : null;

  return (
    <Layout>
      <Layout.TopBar center={<Headline4>뇌피셜 초시계</Headline4>} />
      <Layout.Content>
        <S.Container>
          <S.TargetSection>
            <TargetTime targetTimeMillis={targetTimeMillis} />
          </S.TargetSection>
          <S.TimerSection>
            <TimerDisplay
              displayTime={displayTime}
              isBlind={isBlind}
              isStopped={isStopped}
              stoppedTimeDisplay={stoppedTimeDisplay}
              onStop={handleStop}
            />
          </S.TimerSection>
          <S.StatusSection>
            <PlayerStatusBoard players={progressData.players} myName={myName} />
          </S.StatusSection>
        </S.Container>
      </Layout.Content>
      {gameState === 'PREPARE' && <PrepareOverlay />}
    </Layout>
  );
};

export default BlindTimerGamePlayPage;
