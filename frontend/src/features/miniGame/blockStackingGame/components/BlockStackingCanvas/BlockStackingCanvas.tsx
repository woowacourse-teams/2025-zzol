import { PointerEvent, useCallback, useEffect, useRef, useState } from 'react';
import { useBlockStackingGameContext } from '@/contexts/BlockStackingGame/BlockStackingGameContext';
import { useBlockStackingGame } from '../../hooks/useBlockStackingGame';
import { useBlockStackingActions } from '../../hooks/useBlockStackingActions';
import { useBlockStackingSounds } from '../../hooks/useBlockStackingSounds';
import BlockStackingRanks from '../BlockStackingRanks/BlockStackingRanks';
import * as S from './BlockStackingCanvas.styled';

const BlockStackingCanvas = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const { gameState, isLocalGameOver, setLocalGameOver, endTimeEpochMs } =
    useBlockStackingGameContext();
  const [muted, setMuted] = useState(false);

  const sounds = useBlockStackingSounds(muted);
  const { publishProgress } = useBlockStackingActions();
  const { handleTap, timeLeft } = useBlockStackingGame(
    canvasRef,
    gameState,
    isLocalGameOver,
    endTimeEpochMs,
    {
      setLocalGameOver,
      sounds,
      onBlockPlaced: publishProgress,
    }
  );

  // 캔버스 크기를 부모 요소에 맞게 조정
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const updateSize = () => {
      const parent = canvas.parentElement;
      if (!parent) return;
      canvas.width = parent.clientWidth;
      canvas.height = parent.clientHeight;
    };

    updateSize();
    window.addEventListener('resize', updateSize);
    return () => window.removeEventListener('resize', updateSize);
  }, []);

  const handleMutePointerDown = useCallback((e: PointerEvent<HTMLButtonElement>) => {
    // 래퍼의 pointerdown으로 버블링되어 handleTap이 호출되지 않도록 차단
    e.stopPropagation();
    setMuted((prev) => !prev);
  }, []);

  return (
    <S.Wrapper>
      <S.GameContainer onPointerDown={handleTap}>
        <S.Canvas ref={canvasRef} />
        <BlockStackingRanks />
        <S.MuteButton onPointerDown={handleMutePointerDown}>
          {muted ? '소리 켜기' : '소리 끄기'}
        </S.MuteButton>
      </S.GameContainer>
      <S.TimerContainer>
        <S.TimerFill timeLeft={timeLeft} totalTime={20} />
      </S.TimerContainer>
    </S.Wrapper>
  );
};

export default BlockStackingCanvas;
