import { usePrefersReducedMotion } from '@/hooks/usePrefersReducedMotion';
import { useEffect, useRef, RefObject } from 'react';

const BACKGROUND_SPEED_MULTIPLIER = 10;
const SPEED_SMOOTHING_FACTOR = 0.08;
/** 이 아래로는 배경이 멈춰 보인다. 스타일 쓰기를 건너뛴다. */
const MIN_RENDER_SPEED = 0.05;

type Props = {
  containerRef: RefObject<HTMLDivElement | null>;
  mySpeed: number;
};

export const useBackgroundAnimation = ({ containerRef, mySpeed }: Props) => {
  const backgroundPositionRef = useRef(0);
  const currentSpeedRef = useRef(0);
  const mySpeedRef = useRef(mySpeed);
  const prefersReducedMotion = usePrefersReducedMotion();

  // 서버가 100ms 마다 속도를 내려준다. 값만 갈아 끼우고 루프는 마운트 때 한 번만 건다.
  useEffect(() => {
    mySpeedRef.current = mySpeed;
  }, [mySpeed]);

  useEffect(() => {
    if (prefersReducedMotion) return;

    let frameId: number;
    let lastTime = performance.now();

    const update = (time: number) => {
      //delta : 현재 프레임과 이전 프레임의 시간 차이
      //일정한 속도로 애니메이션을 진행하기 위해 필요
      const delta = (time - lastTime) / 1000; // 초 단위
      lastTime = time;

      // Lerp를 사용하여 현재 속도를 목표 속도로 부드럽게 전환
      currentSpeedRef.current +=
        (mySpeedRef.current - currentSpeedRef.current) * SPEED_SMOOTHING_FACTOR;

      if (currentSpeedRef.current > MIN_RENDER_SPEED) {
        // background-position 의 백분율은 이미지의 그 지점을 컨테이너의 같은 지점에 맞춘다.
        // 이미지가 컨테이너보다 넓으므로 값이 커질수록 이미지는 왼쪽으로 밀린다.
        // 세상이 왼쪽으로 흘러야 내가 앞으로 달리는 것으로 읽히므로 값을 키운다.
        backgroundPositionRef.current +=
          currentSpeedRef.current * delta * BACKGROUND_SPEED_MULTIPLIER;
        if (containerRef.current) {
          containerRef.current.style.backgroundPosition = `${backgroundPositionRef.current}% center`;
        }
      }

      frameId = requestAnimationFrame(update);
    };

    frameId = requestAnimationFrame(update);
    return () => cancelAnimationFrame(frameId);
  }, [containerRef, prefersReducedMotion]);

  return backgroundPositionRef;
};
