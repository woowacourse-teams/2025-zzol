import { useEffect, useRef, RefObject } from 'react';

const BACKGROUND_SPEED_MULTIPLIER = 10;
const SPEED_SMOOTHING_FACTOR = 0.08;

type Props = {
  containerRef: RefObject<HTMLDivElement | null>;
  mySpeed: number;
};

export const useBackgroundAnimation = ({ containerRef, mySpeed }: Props) => {
  const backgroundPositionRef = useRef(0);
  const currentSpeedRef = useRef(0);

  useEffect(() => {
    let frameId: number;
    let lastTime = performance.now();

    const update = (time: number) => {
      //delta : 현재 프레임과 이전 프레임의 시간 차이
      //일정한 속도로 애니메이션을 진행하기 위해 필요
      const delta = (time - lastTime) / 1000; // 초 단위
      lastTime = time;

      // Lerp를 사용하여 현재 속도를 목표 속도로 부드럽게 전환
      currentSpeedRef.current += (mySpeed - currentSpeedRef.current) * SPEED_SMOOTHING_FACTOR;

      backgroundPositionRef.current +=
        currentSpeedRef.current * delta * BACKGROUND_SPEED_MULTIPLIER;
      if (containerRef.current) {
        containerRef.current.style.backgroundPosition = `${backgroundPositionRef.current}% center`;
      }

      frameId = requestAnimationFrame(update);
    };

    frameId = requestAnimationFrame(update);
    return () => cancelAnimationFrame(frameId);
  }, [mySpeed, containerRef]);

  return backgroundPositionRef;
};
