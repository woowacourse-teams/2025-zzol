import type { PointerEvent } from 'react';
import { useCallback } from 'react';
import { useCancelablePress } from '@/hooks/useCancelablePress';
import { usePressAnimation } from '@/hooks/usePressAnimation';

type Props = {
  onClick?: () => void;
};

const RELEASE_SYNC_DELAY_MS = 100;

export const useButtonInteraction = ({ onClick }: Props = {}) => {
  const {
    touchState,
    onPointerDown: onPointerDownAnimation,
    onPointerUp: onPointerUpAnimation,
    onPointerCancel: onPointerCancelAnimation,
  } = usePressAnimation({ releaseSyncDelay: RELEASE_SYNC_DELAY_MS });

  const {
    moved,
    onPointerDown: onPointerDownCancel,
    onPointerMove: onPointerMoveCancel,
    onPointerCancel: onPointerCancelPress,
    onPointerUp: onPointerUpCancel,
  } = useCancelablePress({
    onClick,
    releaseSyncDelay: RELEASE_SYNC_DELAY_MS,
  });

  const onPointerDown = useCallback(
    (e: PointerEvent<HTMLElement>) => {
      if (e.pointerType !== 'touch') return;

      onPointerDownAnimation(e);
      onPointerDownCancel(e);
    },
    [onPointerDownAnimation, onPointerDownCancel]
  );

  const onPointerMove = useCallback(
    (e: PointerEvent<HTMLElement>) => {
      if (e.pointerType !== 'touch') return;

      onPointerMoveCancel(e);

      if (moved.current && touchState === 'pressing') {
        onPointerCancelAnimation(e);
      }
    },
    [onPointerMoveCancel, moved, touchState, onPointerCancelAnimation]
  );

  const onPointerCancel = useCallback(
    (e: PointerEvent<HTMLElement>) => {
      if (e.pointerType !== 'touch') return;

      onPointerCancelAnimation(e);
      onPointerCancelPress(e);
    },
    [onPointerCancelAnimation, onPointerCancelPress]
  );

  const onPointerUp = useCallback(
    (e: PointerEvent<HTMLElement>) => {
      if (e.pointerType === 'touch') {
        suppressNextClick();
        onPointerUpAnimation(e);
        onPointerUpCancel(e);
      } else {
        onClick?.();
      }
    },
    [onClick, onPointerUpAnimation, onPointerUpCancel]
  );

  return {
    touchState,
    pointerHandlers: {
      onPointerDown,
      onPointerMove,
      onPointerCancel,
      onPointerUp,
    },
  };
};

const suppressNextClick = () => {
  const handler = (e: MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
  };

  document.addEventListener('click', handler, true);
  requestAnimationFrame(() => {
    document.removeEventListener('click', handler, true);
  });
};
