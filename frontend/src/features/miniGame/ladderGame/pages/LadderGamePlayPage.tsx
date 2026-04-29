import Headline4 from '@/components/@common/Headline4/Headline4';
import { useLadderGameContext } from '@/contexts/LadderGame/LadderGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { useEffect, useRef, useState, memo } from 'react';
import { useParams } from 'react-router-dom';
import PrepareOverlay from '../../components/PrepareOverlay/PrepareOverlay';
import LadderBoard from '../components/LadderBoard/LadderBoard';
import * as S from './LadderGamePlayPage.styled';

const TimerBar = memo(({ endTimeEpochMs }: { endTimeEpochMs: number | null }) => {
  const [timeLeft, setTimeLeft] = useState(0);
  const totalMsRef = useRef<number>(1);
  const endTimeRef = useRef<number | null>(endTimeEpochMs);

  useEffect(() => {
    endTimeRef.current = endTimeEpochMs;
    if (endTimeEpochMs) {
      totalMsRef.current = Math.max(1, endTimeEpochMs - Date.now());
    } else {
      setTimeLeft(0);
    }
  }, [endTimeEpochMs]);

  useEffect(() => {
    let rafId: number;
    const tick = () => {
      if (endTimeRef.current !== null && totalMsRef.current > 0) {
        const remaining = Math.max(0, (endTimeRef.current - Date.now()) / 1000);
        setTimeLeft(remaining);
      }
      rafId = requestAnimationFrame(tick);
    };
    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, []);

  if (!endTimeEpochMs) return null;

  return (
    <S.TimerBarWrapper>
      <S.TimerBarFill $timeLeft={timeLeft} $totalTime={totalMsRef.current / 1000} />
    </S.TimerBarWrapper>
  );
});

TimerBar.displayName = 'TimerBar';

const LadderGamePlayPage = () => {
  const { joinCode } = useIdentifier();
  const { gameState, endTimeEpochMs } = useLadderGameContext();
  const navigate = useReplaceNavigate();
  const { miniGameType } = useParams();

  useEffect(() => {
    if (gameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [gameState, joinCode, navigate, miniGameType]);

  return (
    <Layout>
      <Layout.TopBar center={<Headline4>사다리 게임</Headline4>} />
      <Layout.Content>
        <S.Container>
          {gameState === 'DRAWING' && <TimerBar endTimeEpochMs={endTimeEpochMs ?? null} />}
          <S.BoardWrapper>
            {['PREPARE', 'DRAWING', 'RESULT'].includes(gameState) && <LadderBoard />}
          </S.BoardWrapper>
        </S.Container>
      </Layout.Content>
      {gameState === 'PREPARE' && <PrepareOverlay />}
    </Layout>
  );
};

export default LadderGamePlayPage;
