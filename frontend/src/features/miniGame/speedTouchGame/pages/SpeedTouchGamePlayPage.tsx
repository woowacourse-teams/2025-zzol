import Headline4 from '@/components/@common/Headline4/Headline4';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useSpeedTouchGame } from '@/contexts/SpeedTouchGame/SpeedTouchGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import PrepareOverlay from '../../components/PrepareOverlay/PrepareOverlay';
import ProgressBoard from '../components/ProgressBoard/ProgressBoard';
import TimerBar from '../components/TimerBar/TimerBar';
import TouchGrid from '../components/TouchGrid/TouchGrid';
import { useCountdown } from '../hooks/useCountdown';
import { useGridNumbers } from '../hooks/useGridNumbers';
import { useSpeedTouchActions } from '../hooks/useSpeedTouchActions';
import * as S from './SpeedTouchGamePlayPage.styled';

const SpeedTouchGamePlayPage = () => {
  const { joinCode, myName } = useIdentifier();
  const { gameState, progressData } = useSpeedTouchGame();
  const { sendTouch } = useSpeedTouchActions();
  const navigate = useReplaceNavigate();
  const { miniGameType } = useParams();
  const numbers = useGridNumbers();

  const [nextNumber, setNextNumber] = useState(1);
  const [isFinished, setIsFinished] = useState(false);

  const isPlaying = gameState === 'PLAYING';
  const { seconds, progress } = useCountdown(isPlaying);

  const handleTouch = useCallback(
    (number: number) => {
      if (isFinished || !isPlaying) return;
      if (number !== nextNumber) return;

      sendTouch(number);
      setNextNumber((prev) => prev + 1);

      if (number === 25) {
        setIsFinished(true);
      }
    },
    [isFinished, isPlaying, nextNumber, sendTouch]
  );

  useEffect(() => {
    if (gameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [gameState, joinCode, navigate, miniGameType]);

  return (
    <Layout>
      <Layout.TopBar center={<Headline4>1 to 25</Headline4>} />
      <Layout.Content>
        <S.Container>
          {isPlaying && (
            <S.TimerSection>
              <TimerBar seconds={seconds} progress={progress} />
            </S.TimerSection>
          )}
          <S.GridSection>
            <TouchGrid
              numbers={numbers}
              nextNumber={nextNumber}
              isFinished={isFinished}
              onTouch={handleTouch}
            />
          </S.GridSection>
          <S.ProgressSection>
            <ProgressBoard players={progressData.players} myName={myName} />
          </S.ProgressSection>
        </S.Container>
      </Layout.Content>
      {gameState === 'PREPARE' && <PrepareOverlay />}
    </Layout>
  );
};

export default SpeedTouchGamePlayPage;
