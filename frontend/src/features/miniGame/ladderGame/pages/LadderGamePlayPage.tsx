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
  }, [endTimeEpochMs]);

  useEffect(() => {
    let rafId: number;
    let totalSec = 0;
    const tick = () => {
      if (endTimeRef.current !== null) {
        // 총 길이(분모)는 첫 유효 프레임에 한 번만 확정한다 — rAF 콜백이므로 렌더 순수성에 영향 없음.
        if (totalSec === 0) {
          totalSec = Math.max(1, endTimeRef.current - Date.now()) / 1000;
          setTotalTimeSec(totalSec);
        }
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
