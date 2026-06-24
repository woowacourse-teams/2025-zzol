import { useEffect, useRef } from 'react';

const ROTATION_SPEED_MULTIPLIER = 22;
const SPEED_SMOOTHING_FACTOR = 0.08;

type Props = {
  speed: number;
};

export const useRotationAnimation = ({ speed }: Props) => {
  const rotatingRef = useRef<HTMLDivElement>(null);
  const angleRef = useRef(0);
  const currentSpeedRef = useRef(0);
  const speedRef = useRef(speed);

  useEffect(() => {
    speedRef.current = speed;
  }, [speed]);

  useEffect(() => {
    let frameId: number;
    let lastTime = performance.now();

    const update = (time: number) => {
      const delta = (time - lastTime) / 1000;
      lastTime = time;

      currentSpeedRef.current +=
        (speedRef.current - currentSpeedRef.current) * SPEED_SMOOTHING_FACTOR;

      angleRef.current += currentSpeedRef.current * delta * 10 * ROTATION_SPEED_MULTIPLIER;
      if (rotatingRef.current) {
        rotatingRef.current.style.transform = `rotate(${angleRef.current}deg)`;
      }

      frameId = requestAnimationFrame(update);
    };

    frameId = requestAnimationFrame(update);
    return () => cancelAnimationFrame(frameId);
  }, []);

  return rotatingRef;
};
