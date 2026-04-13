import { PointerEvent, useCallback, useEffect, useRef } from 'react';
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
  const sounds = useBlockStackingSounds();
  const { muted, toggleMute } = sounds;
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

  // 캔버스 크기를 부모 요소에 맞게 조정 (마운트 시 1회 등록)
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

  // 키보드 입력 처리 (handleTap 변경 시에만 재등록)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.code === 'Space' || e.key === 'Enter') {
        e.preventDefault();
        handleTap();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleTap]);

  const handleMutePointerDown = useCallback((e: PointerEvent<HTMLButtonElement>) => {
    // 래퍼의 pointerdown으로 버블링되어 handleTap이 호출되지 않도록 차단
    e.stopPropagation();
    toggleMute();
  }, [toggleMute]);

  return (
    <S.Wrapper>
      <S.GameContainer onPointerDown={handleTap} role="application" aria-label="블록쌓기 게임 영역">
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
