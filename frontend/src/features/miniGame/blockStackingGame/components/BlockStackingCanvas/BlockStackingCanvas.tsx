import { PointerEvent, useCallback, useEffect, useRef, useState } from 'react';
import { useBlockStackingGameContext } from '@/contexts/BlockStackingGame/BlockStackingGameContext';
import { useBlockStackingGame } from '../../hooks/useBlockStackingGame';
import { useBlockStackingActions } from '../../hooks/useBlockStackingActions';
import { useBlockStackingSounds } from '../../hooks/useBlockStackingSounds';
import BlockStackingRanks from '../BlockStackingRanks/BlockStackingRanks';
import * as S from './BlockStackingCanvas.styled';

const BlockStackingCanvas = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const { gameState, isLocalGameOver, setLocalGameOver, endTimeEpochMs, totalTimeSeconds } =
    useBlockStackingGameContext();
  const [muted, setMuted] = useState(false);

  const sounds = useBlockStackingSounds(muted);
  const { publishProgress, publishFail } = useBlockStackingActions();
  const { handleTap, timeLeft } = useBlockStackingGame(
    canvasRef,
    gameState,
    isLocalGameOver,
    endTimeEpochMs,
    {
      setLocalGameOver,
      sounds,
      onBlockPlaced: publishProgress,
      onFail: publishFail,
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

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.code === 'Space' || e.key === 'Enter') {
        // 게임 컨테이너가 보이거나 플레이 중일 때만 동작하도록 추가적인 조건이 필요하다면 여기에 추가
        e.preventDefault();
        handleTap();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      window.removeEventListener('resize', updateSize);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [handleTap]);

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
        <S.TimerFill timeLeft={timeLeft} totalTime={totalTimeSeconds} />
      </S.TimerContainer>
    </S.Wrapper>
  );
};

export default BlockStackingCanvas;
