import Headline4 from '@/components/@common/Headline4/Headline4';
import { useLadderGameContext } from '@/contexts/LadderGame/LadderGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import PrepareOverlay from '../../components/PrepareOverlay/PrepareOverlay';
import LadderBoard from '../components/LadderBoard/LadderBoard';
import * as S from './LadderGamePlayPage.styled';

type TimerBarProps = { endTimeEpochMs: number | null };

const TimerBar = ({ endTimeEpochMs }: TimerBarProps) => {
  const [timeLeft, setTimeLeft] = useState(0);
  const [totalTimeSec, setTotalTimeSec] = useState(0.001);
  const endTimeRef = useRef<number | null>(endTimeEpochMs);

  useEffect(() => {
    endTimeRef.current = endTimeEpochMs;
    // 총 길이(분모)는 서버가 정하는 종료 시각이 바뀔 때마다 재계산해야 한다 — 렌더 순수성을 위해 effect 에서 파생.
    if (endTimeEpochMs) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setTotalTimeSec(Math.max(1, endTimeEpochMs - Date.now()) / 1000);
    }
  }, [endTimeEpochMs]);

  useEffect(() => {
    let rafId: number;
    const tick = () => {
      if (endTimeRef.current !== null) {
        const remaining = Math.max(0, (endTimeRef.current - Date.now()) / 1000);
        setTimeLeft(remaining);
        if (remaining > 0) {
          rafId = requestAnimationFrame(tick);
          return;
        }
      }
    };
    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, []);

  if (!endTimeEpochMs) return null;

  return (
    <S.TimerBarWrapper>
      <S.TimerBarFill $timeLeft={timeLeft} $totalTime={totalTimeSec} />
    </S.TimerBarWrapper>
  );
};

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
