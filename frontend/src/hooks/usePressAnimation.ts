import type { PointerEvent } from 'react';

import { useState, useCallback } from 'react';

import type { TouchState } from '@/types/touchState';

type Props = {
  releaseSyncDelay?: number;
};

export const usePressAnimation = ({ releaseSyncDelay = 100 }: Props = {}) => {
  const [touchState, setTouchState] = useState<TouchState>('idle');

  const onPointerDown = useCallback((e: PointerEvent<HTMLElement>) => {
    if (e.pointerType !== 'touch') return;

    setTouchState('pressing');
  }, []);

  const onPointerUp = useCallback(
    (e: PointerEvent<HTMLElement>) => {
      if (e.pointerType !== 'touch') return;

      setTouchState('releasing');
      setTimeout(() => {
        setTouchState('idle');
      }, releaseSyncDelay);
    },
    [releaseSyncDelay]
  );

  const onPointerCancel = useCallback((e: PointerEvent<HTMLElement>) => {
    if (e.pointerType !== 'touch') return;

    setTouchState('idle');
  }, []);

  return {
    touchState,
    onPointerDown,
    onPointerUp,
    onPointerCancel,
  };
};
