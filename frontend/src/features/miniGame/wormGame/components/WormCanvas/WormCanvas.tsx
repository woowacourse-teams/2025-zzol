import { useTheme } from '@emotion/react';
import { useCallback, useEffect, useMemo, useRef } from 'react';
import * as S from './WormCanvas.styled';
import { colorList } from '@/constants/color';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { useWormGame } from '@/contexts/WormGame/WormGameContext';
import { WormRenderer } from '../../core/wormRenderer';
import { useWormRenderLoop } from '../../hooks/useWormRenderLoop';

/**
 * 지렁이 게임 메인 뷰(추적 카메라 + 미니맵). 델타·궤적은 WormStore(ref) 에서 rAF 로 읽는다.
 * 조향 입력·관전 전환 UI 는 상위 페이지가 store 에 쓴다(4단계).
 */
const WormCanvas = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const theme = useTheme();
  const { store } = useWormGame();
  const { getParticipantColorIndex } = useParticipants();

  // 색의 원천은 서버 Player.colorIndex. 리프레시 시 명단 복구는 #1688(공통)이 담당한다
  const colorOf = useCallback(
    (name: string) => colorList[getParticipantColorIndex(name)],
    [getParticipantColorIndex]
  );

  // roster 갱신마다 renderer 를 재생성하면 레이어·카메라·페이드가 리셋된다 — 인스턴스는 유지하고 색 조회만 교체
  const renderer = useMemo(
    () =>
      new WormRenderer(store, colorOf, {
        background: theme.color.gray[900],
        arena: theme.color.gray[800],
        ring: theme.color.gray[500],
        head: theme.color.white,
        minimapBg: `${theme.color.gray[950]}B3`,
        viewport: `${theme.color.white}80`,
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- colorOf 는 아래 effect 로 갱신
    [store, theme]
  );

  useEffect(() => {
    renderer.setColorOf(colorOf);
  }, [renderer, colorOf]);

  useWormRenderLoop(canvasRef, renderer);

  return (
    <S.Container>
      <S.Canvas ref={canvasRef} aria-label="지렁이 게임 아레나" />
    </S.Container>
  );
};

export default WormCanvas;
