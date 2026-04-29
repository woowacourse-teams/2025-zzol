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

const LadderGamePlayPage = () => {
  const { joinCode } = useIdentifier();
  const { gameState, endTimeEpochMs } = useLadderGameContext();
  const navigate = useReplaceNavigate();
  const { miniGameType } = useParams();

  const [remainingRatio, setRemainingRatio] = useState(1);
  const endTimeRef = useRef<number | null>(null);
  const totalMsRef = useRef<number>(0);

  useEffect(() => {
    if (gameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [gameState, joinCode, navigate, miniGameType]);

  // ref 동기화: gameState/endTimeEpochMs 변경 시에만 실행
  useEffect(() => {
    if (gameState === 'DRAWING' && endTimeEpochMs) {
      endTimeRef.current = endTimeEpochMs;
      totalMsRef.current = Math.max(1, endTimeEpochMs - Date.now());
    } else {
      endTimeRef.current = null;
      setRemainingRatio(1);
    }
  }, [gameState, endTimeEpochMs]);

  // RAF 루프는 마운트 시 한 번만 시작 — 리렌더/클릭에 영향받지 않음
  useEffect(() => {
    let rafId: number;
    const tick = () => {
      if (endTimeRef.current !== null && totalMsRef.current > 0) {
        const ratio = Math.max(0, (endTimeRef.current - Date.now()) / totalMsRef.current);
        setRemainingRatio(ratio);
      }
      rafId = requestAnimationFrame(tick);
    };
    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, []);

  return (
    <Layout>
      <Layout.TopBar center={<Headline4>사다리 게임</Headline4>} />
      <Layout.Content>
        <S.Container>
          {gameState === 'DRAWING' && (
            <S.TimerBarWrapper>
              <S.TimerBarFill $ratio={remainingRatio} />
            </S.TimerBarWrapper>
          )}
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
