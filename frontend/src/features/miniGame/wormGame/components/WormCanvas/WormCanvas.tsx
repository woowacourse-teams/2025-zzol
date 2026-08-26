import { useTheme } from '@emotion/react';
import { useCallback, useMemo, useRef } from 'react';
import * as S from './WormCanvas.styled';
import { colorList } from '@/constants/color';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { useWormGame } from '@/contexts/WormGame/WormGameContext';
import { WormRenderer } from '../../core/wormRenderer';
import { useWormRenderLoop } from '../../hooks/useWormRenderLoop';

/** roster 가 비었을 때(하드 리프레시) 전원 0번 색이 되는 것을 피하는 이름 해시 폴백(NunchiCrowd 선례) */
const fallbackColorIndex = (name: string): number => {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) % colorList.length;
  return hash;
};

/**
 * 지렁이 게임 메인 뷰(추적 카메라 + 미니맵). 델타·궤적은 WormStore(ref) 에서 rAF 로 읽는다.
 * 조향 입력·관전 전환 UI 는 상위 페이지가 store 에 쓴다(4단계).
 */
const WormCanvas = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const theme = useTheme();
  const { store } = useWormGame();
  const { participants, getParticipantColorIndex } = useParticipants();

  const colorOf = useCallback(
    (name: string) =>
      colorList[participants.length ? getParticipantColorIndex(name) : fallbackColorIndex(name)],
    [participants.length, getParticipantColorIndex]
  );

  const renderer = useMemo(
    () =>
      new WormRenderer(store, colorOf, {
        background: theme.color.gray[900],
        arena: theme.color.gray[800],
        ring: theme.color.gray[500],
        dead: theme.color.gray[400],
        minimapBg: `${theme.color.gray[950]}B3`,
        viewport: `${theme.color.white}80`,
      }),
    [store, colorOf, theme]
  );

  useWormRenderLoop(canvasRef, renderer);

  return (
    <S.Container>
      <S.Canvas ref={canvasRef} aria-label="지렁이 게임 아레나" />
    </S.Container>
  );
};

export default WormCanvas;
